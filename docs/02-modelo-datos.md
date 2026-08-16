# Data Model

> Schema is managed by **Flyway** migrations `V1 → V19` (see
> `backend/pedidos/src/main/resources/db/migration`). JPA is configured with
> `ddl-auto=validate`, so the database is the source of truth and the schema
> must always match the entities.

---

## Entity relationships (overview)

```
usuario ──< usuario_roles (composite)
cliente ──< pedido >── pedido_item >── item
zona ──< cliente                item ──< lote >── proveedor
zona ──< ruta >── ruta_pedido ──< pedido        item >── categoria
pedido ──< pedido (self-FK pedido_padre_id)      lote >── movimiento_stock
pedido ──< remito ──< remito_linea >── item
cliente ──< cobranza ──< pedido (nullable)
proveedor ──< orden_compra ──< orden_compra_linea >── item
pedido ──< sustitucion (item_original / item_sustituto)
pedido ──< notificacion ──< usuario (para_usuario)
```

Legend: `>` = FK to the referenced table; `──<` = one-to-many (the `<` is on
the "many" side).

---

## Tables by migration

### V1 — `zona`
| Column | Type / Notes |
|---|---|
| `id` | PK |
| `nombre` | UNIQUE |
| `activo` | soft-delete flag |

### V2 — `usuario` + `usuario_roles`
| Table | Columns |
|---|---|
| `usuario` | `id`, `nombre`, `email` (UNIQUE), `password_hash`, `activo` |
| `usuario_roles` | `usuario_id` (FK CASCADE), `rol` — composite |

### V3 — `cliente`
`razon_social`, `cuit` (UNIQUE), `email`, `telefono`, `domicilio`,
`zona_id` (FK), `activo`.

### V4 — `item`, `lote`, `movimiento_stock`
| Table | Columns |
|---|---|
| `item` | `sku` (UNIQUE), `nombre`, `unidad_medida`, `activo` |
| `lote` | `item_id` (FK), `codigo_lote`, `fecha_ingreso`, `fecha_vencimiento`, `cantidad_ingresada` |
| `movimiento_stock` | `tipo`, `item_id` (FK), `lote_id` (FK, nullable), `pedido_id`, `cantidad`, `fecha`, `motivo` |

### V5 — `pedido`, `pedido_item`
| Table | Columns |
|---|---|
| `pedido` | `numero` (UNIQUE), `cliente_id` (FK), `vendedor_id` (FK), `pedido_padre_id` (FK self), `estado`, `fecha_creacion`, `fecha_jornada`, `observaciones`, `total` |
| `pedido_item` | `pedido_id` (FK CASCADE), `item_id` (FK), `cantidad_pedida`, `cantidad_reservada`, `cantidad_entregada`, `precio_unitario`, `pendiente_stock` |

### V6 — `ruta`, `ruta_pedido`
| Table | Columns |
|---|---|
| `ruta` | `zona_id` (FK), `repartidor_id` (FK), `fecha_jornada`, `estado` |
| `ruta_pedido` | `ruta_id` (FK CASCADE), `pedido_id` (FK), UNIQUE(`ruta_id`,`pedido_id`) |

### V7 — `seguridad`
Enables `pgcrypto` extension and seeds BCrypt password hashes.

### V8 — `item.stock_minimo`
Adds `stock_minimo` to `item` (used for low-stock alerts).

### V9 — `remito`, `remito_linea`, `cobranza`
| Table | Columns |
|---|---|
| `remito` | `numero` (UNIQUE), `pedido_id` (FK), `cliente_id` (FK), `fecha_emision`, `monto_total` |
| `remito_linea` | `remito_id` (FK CASCADE), `item_id` (FK), `cantidad`, `precio_unitario`, `subtotal` |
| `cobranza` | `cliente_id` (FK), `pedido_id` (FK, nullable), `monto`, `forma_pago`, `fecha`, `observaciones` |

### V10 — `proveedor`, `orden_compra`, `orden_compra_linea`
| Table | Columns |
|---|---|
| `proveedor` | `razon_social`, `cuit` (UNIQUE), `email`, `telefono`, `activo` |
| `orden_compra` | `numero` (UNIQUE), `proveedor_id` (FK), `fecha`, `estado`, `observaciones` |
| `orden_compra_linea` | `orden_compra_id` (FK CASCADE), `item_id` (FK), `cantidad_pedida`, `cantidad_recibida`, `precio_unitario` |

### V11 — `item.precio_lista`
Adds list price to `item`.

### V13 — `ruta.capacidad_bultos`
Adds capacity (in crates/bulk units) to a route.

### V14 — `notificacion`
`tipo`, `mensaje`, `para_usuario_id` (FK), `pedido_id` (nullable), `leida`, `fecha`.

### V15 — `sustitucion`
`pedido_id` (FK), `item_original_id` (FK), `item_sustituto_id` (FK), `cantidad`, `diferencia_precio`, `fecha`, `observaciones`.

### V16 — `pedido.express`
Adds `express BOOLEAN DEFAULT FALSE` to `pedido` plus index
`(estado, express DESC, fecha_creacion ASC)` to prioritize express orders in the
preparation/dispatch queue.

### V17 — `categoria`
Creates `categoria` (`id`, `nombre` UNIQUE, `activo`, `fecha_creacion`), adds
`item.categoria_id` (FK), backfills it from the legacy free-text
`item.categoria` column, then `DROP COLUMN categoria`. **Clean cut — no legacy
text category remains.**

### V18 — `lote.proveedor_id`
Adds nullable `proveedor_id` (FK) to `lote` plus index (lot provenance).

### V19 — `lote.estado`
Adds `estado VARCHAR(20) DEFAULT 'VIGENTE'` with
`CHECK (estado IN ('VIGENTE','AGOTADO','VENCIDO','DESCARTADO'))`.

---

## Enums

| Enum | Values |
|---|---|
| `EstadoPedido` | `PENDIENTE_CONFIRMACION`, `PENDIENTE_STOCK`, `PENDIENTE_PREPARACION`, `PENDIENTE_ENTREGA`, `EN_VIAJE`, `ENTREGADO`, `ENTREGADO_PARCIAL`, `RE_AGENDADO`, `RECHAZADO` |
| `EstadoRuta` | `PLANIFICADA`, `EN_CURSO`, `FINALIZADA` |
| `LoteEstado` | `VIGENTE`, `AGOTADO`, `VENCIDO`, `DESCARTADO` |
| `Rol` | `VENDEDOR`, `ENCARGADO_DEPOSITO`, `REPARTIDOR`, `ADMINISTRATIVO` |
| `TipoMovimiento` | `INGRESO`, `RESERVA_PEDIDO`, `LIBERACION_RESERVA`, `EGRESO_VENTA`, `MERMA`, `AJUSTE_INVENTARIO` |
| `FormaPago` | `EFECTIVO`, `TRANSFERENCIA`, `TARJETA`, `OTRO` |
| `EstadoOrdenCompra` | `PENDIENTE`, `RECIBIDA_PARCIAL`, `RECIBIDA`, `CANCELADA` |

---

## Lifecycle: order (`pedido`)

```
crear           → PENDIENTE_CONFIRMACION
confirmar       → PENDIENTE_PREPARACION  (or PENDIENTE_STOCK if any item lacks stock)
despachar       → PENDIENTE_ENTREGA
iniciarViaje    → EN_VIAJE
registrarEntrega→ ENTREGADO or ENTREGADO_PARCIAL (generates child order with remainder)
reAgendar       → RE_AGENDADO
rechazar        → RECHAZADO  (releases stock reservations)
consolidar      → new PENDIENTE_CONFIRMACION order + sources become RECHAZADO
TTL job (48h)   → auto-cancels inactive orders
```

States eligible for collection (`cobranza`): `EN_VIAJE`, `ENTREGADO`,
`ENTREGADO_PARCIAL`.

## Lifecycle: lot (`lote`)

```
born            → VIGENTE
egreso agota    → AGOTADO   (no remaining quantity)
descartar       → DESCARTADO (books remaining stock as merma)
VENCIDO         → derived at read time (excluded from FEFO selection)
```

---

## Soft-delete

Soft-delete uses an `activo` boolean flag on `zona`, `usuario`, `cliente`,
`item`, `proveedor` and `categoria`. Business rules enforce it:

| Entity inactive | Effect |
|---|---|
| `item` | No stock operations; cannot be added to orders → `ITEM_INACTIVO` |
| `cliente` | Cannot receive new orders → `CLIENTE_INACTIVO` |
| `proveedor` | Purchase orders blocked → `PROVEEDOR_INACTIVO` |
| `usuario` | Cannot authenticate → `AUTH_INACTIVO` |

---

Continue to the [API reference](./03-api.md) or the [flows](./04-flujos.md).
