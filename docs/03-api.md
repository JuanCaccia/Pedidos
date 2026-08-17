# API Reference

> Base path for every endpoint: `/api` (server `context-path`). For example
> login is `POST /api/auth/login`. Interactive docs are available through
> OpenAPI/Swagger at `/api/swagger-ui/` and `/api/v3/api-docs/**`.

All endpoints are JSON unless noted (CSV exports). Authorization is
role-based; see [05-seguridad.md](./05-seguridad.md) for the full matrix.

---

## Endpoints by module

### Auth
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/auth/login` | Obtain a JWT | Public |

### Usuario
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/usuarios` | Create user | ADMINISTRATIVO |
| POST | `/usuarios/{id}/roles` | Assign roles | ADMINISTRATIVO |
| PATCH | `/usuarios/{id}/desactivar` | Deactivate (soft) | ADMINISTRATIVO |
| PATCH | `/usuarios/{id}/reactivar` | Reactivate | ADMINISTRATIVO |
| PUT | `/usuarios/{id}/password` | Change password | ADMINISTRATIVO |
| GET | `/usuarios/{id}` | Get user | ADMINISTRATIVO |
| GET | `/usuarios` | List/search users | ADMINISTRATIVO |

### Cliente
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/clientes` | Create customer | VENDEDOR, ADMIN |
| PUT | `/clientes/{id}` | Update customer | VENDEDOR, ADMIN |
| PATCH | `/clientes/{id}/desactivar` | Deactivate | VENDEDOR, ADMIN |
| PATCH | `/clientes/{id}/reactivar` | Reactivate | VENDEDOR, ADMIN |
| GET | `/clientes/exportar.csv` | Export CSV | Authenticated |
| GET | `/clientes/{id}` | Get customer | Authenticated |
| GET | `/clientes/buscar/cuit/{cuit}` | Find by CUIT | Authenticated |
| GET | `/clientes` | List/search | Authenticated |

### Zona
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/zonas` | Create zone | ADMIN |
| PUT | `/zonas/{id}` | Update zone | ADMIN |
| PATCH | `/zonas/{id}/desactivar` | Deactivate (soft) | ADMIN |
| PATCH | `/zonas/{id}/reactivar` | Reactivate | ADMIN |
| GET | `/zonas/{id}` | Get zone | Authenticated |
| GET | `/zonas` | List zones | Authenticated |

### Item
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/items` | Create item | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/items` | List/search | Authenticated |
| GET | `/items/{id}` | Get item | Authenticated |
| PUT | `/items/{id}` | Update item | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/items/{id}/desactivar` | Deactivate | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/items/{id}/reactivar` | Reactivate | ENCARGADO_DEPOSITO, ADMIN |

### Stock
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/stock/ingresos` | Record stock receipt (accepts optional `precioUnitario`) | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/stock/ingresos/csv` | Bulk CSV stock receipt (no OC) | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/stock/mermas` | Record wastage (persisted with **negative** sign) | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/stock/ajustes` | Manual adjustment | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/stock/lotes/{id}/descartar` | Discard a lot | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/stock/items/{itemId}` | Item stock snapshot | Authenticated |
| GET | `/stock/items/{itemId}/movimientos` | Movement ledger | Authenticated |
| GET | `/stock/items/{itemId}/lotes` | Lots of an item | Authenticated |
| GET | `/stock/lotes/por-vencer?dias=30` | Expiring-soon lots | Authenticated |
| GET | `/stock/lotes?proveedorId=` | Lots by supplier | Authenticated |

### Pedido
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/pedidos` | Create order | VENDEDOR, ADMIN |
| POST | `/pedidos/{id}/confirmar` | Confirm order | VENDEDOR, ADMIN |
| POST | `/pedidos/{id}/entregas` | Record delivery | REPARTIDOR, ADMIN |
| POST | `/pedidos/{id}/reagendar` | Reschedule order | REPARTIDOR, ADMIN |
| POST | `/pedidos/{id}/despachar` | Dispatch | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/pedidos/{id}/rechazar` | Reject | Authenticated |
| POST | `/pedidos/{id}/agregar-stock` | Add stock to order | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/pedidos/{id}/marcar-faltante` | Mark shortage | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/pedidos/consolidar` | Consolidate child orders | Authenticated |
| GET | `/pedidos/contadores` | Counters by state | Authenticated |
| GET | `/pedidos/exportar.csv?estado=` | Export CSV | Authenticated |
| GET | `/pedidos/{id}` | Get order | Authenticated |
| GET | `/pedidos` | List/search | Authenticated |
| GET | `/pedidos/{id}/hijos` | Child orders | Authenticated |

### Ruta
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/rutas` | Create route | ADMIN (planificación de rutas) |
| POST | `/rutas/{id}/pedidos` | Assign orders | ADMIN |
| POST | `/rutas/{id}/iniciar` | Start route | REPARTIDOR, ADMIN |
| POST | `/rutas/{id}/cerrar` | Close route | REPARTIDOR, ADMIN |
| GET | `/rutas` | List routes | REPARTIDOR, ADMIN |
| GET | `/rutas/{id}` | Get route | REPARTIDOR, ADMIN |

### OrdenCompra (Purchasing)
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/ordenes-compra` | Create purchase order (lines carry **only item + quantity**, no price) | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/ordenes-compra/{id}/recepciones` | Receive (partial/full) — captures **required** `precioUnitario` | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/ordenes-compra/{id}/recepciones/csv` | Bulk CSV receipt linked to an OC | ENCARGADO_DEPOSITO, ADMIN |
| POST | `/ordenes-compra/{id}/cancelar` | Cancel OC | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/ordenes-compra` | List OCs | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/ordenes-compra/{id}` | Get OC | ENCARGADO_DEPOSITO, ADMIN |

### Proveedor
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/proveedores` | Create supplier | ENCARGADO_DEPOSITO, ADMIN |
| PUT | `/proveedores/{id}` | Update supplier | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/proveedores/{id}/desactivar` | Deactivate | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/proveedores/{id}/reactivar` | Reactivate | ENCARGADO_DEPOSITO, ADMIN |
| PUT | `/proveedores/{id}/items` | Set items supplied by the supplier (provision catalog) | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/proveedores/{id}/items` | List items supplied by the supplier | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/proveedores` | List | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/proveedores/{id}` | Get | ENCARGADO_DEPOSITO, ADMIN |

### Cobranza
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/cobranzas` | Record collection (REPARTIDOR exige `pedidoId` de su ruta) | VENDEDOR, ADMIN, REPARTIDOR |
| GET | `/cobranzas` | List collections | Authenticated |
| GET | `/cobranzas/clientes/{id}/cuenta` | Customer account balance | Authenticated |

### Remito
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/remitos` | List remits | Authenticated |
| GET | `/remitos/{id}` | Get remit | Authenticated |

### Sustitucion
| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/sustituciones` | Substitute an item | REPARTIDOR, ADMIN |

### Notificacion
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/notificaciones` | List notifications | ADMIN, ENCARGADO_DEPOSITO |
| GET | `/notificaciones/no-leidas` | Unread notifications | ADMIN, ENCARGADO_DEPOSITO |
| POST | `/notificaciones/{id}/leer` | Mark as read | ADMIN, ENCARGADO_DEPOSITO |

### Reporte
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/reportes/stock` | Stock report | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/reportes/stock/exportar.csv` | Stock CSV | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/reportes/ventas` | Sales report | ADMIN |
| GET | `/reportes/ventas/exportar.csv` | Sales CSV | ADMIN |
| GET | `/reportes/rutas` | Routes report | ADMIN |
| GET | `/reportes/caja` | Cash summary | ADMIN |
| GET | `/reportes/caja/exportar.csv` | Cash CSV | ADMIN |

### Categoria
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/categorias` | List categories | Authenticated |
| POST | `/categorias` | Create category | ENCARGADO_DEPOSITO, ADMIN |
| PUT | `/categorias/{id}` | Update category | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/categorias/{id}/desactivar` | Deactivate | ENCARGADO_DEPOSITO, ADMIN |
| PATCH | `/categorias/{id}/reactivar` | Reactivate | ENCARGADO_DEPOSITO, ADMIN |
| GET | `/categorias/{id}` | Get category | ENCARGADO_DEPOSITO, ADMIN |

### Operations / observability
| Method | Path | Purpose | Role |
|---|---|---|---|
| GET | `/health` | DB connectivity probe (UP/DOWN, 503 on DB down) | Public |
| GET | `/actuator/health` | Actuator health (liveness/readiness, DB details per role) | Public |
| GET | `/actuator/info` | App metadata (`info.app`) | ADMINISTRATIVO |
| GET | `/actuator/metrics` | Actuator metrics | ADMINISTRATIVO |
| GET | `/actuator/prometheus` | Prometheus scrape endpoint (via `micrometer-registry-prometheus`) | ADMINISTRATIVO |
| GET | `/actuator/**` | Other actuator endpoints | ADMINISTRATIVO |
| — | `/v3/api-docs/**`, `/swagger-ui/**` | OpenAPI/Swagger UI | Public |

### Test / developer (not part of the production matrix)

> **Restricted to `dev`/`test` profiles only — does NOT exist in `prod`.** Used by
> the Playwright E2E suite to guarantee a clean, reproducible database per run.

| Method | Path | Purpose | Role |
|---|---|---|---|
| POST | `/test/reset` | TRUNCATE CASCADE over 20 tables + reseed via `DataSeeder.seed()` | Public (`permitAll`, dev/test only) |

---

## Error format

Errors are centralized by a `GlobalExceptionHandler`. Every error returns an
`ApiError` body:

```json
{
  "timestamp": "2026-08-16T15:30:00Z",
  "status": 409,
  "code": "STOCK_INSUFICIENTE",
  "message": "No hay stock suficiente para el item HAR-000",
  "fieldErrors": [
    { "field": "items[0].cantidad", "message": "must be greater than 0" }
  ]
}
```

| Field | Meaning |
|---|---|
| `timestamp` | When the error occurred |
| `status` | HTTP status code |
| `code` | Machine-readable error code |
| `message` | Human-readable description |
| `fieldErrors` | Optional list of field-level validation errors |

### HTTP status mapping

| Condition | Status | `code` |
|---|---|---|
| `BusinessException` | `409 CONFLICT` | `ex.getCode()` (see below) |
| `NotFoundException` | `404 NOT_FOUND` | `NOT_FOUND` |
| `NoResourceFoundException` | `404 NOT_FOUND` | `RESOURCE_NOT_FOUND` |
| Method not supported | `405 METHOD_NOT_ALLOWED` | — |
| Unsupported media type | `415 UNSUPPORTED_MEDIA_TYPE` | — |
| `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `VALIDATION_ERROR` (+ `fieldErrors`) |
| `HttpMessageNotReadableException` | `400 BAD_REQUEST` | `MALFORMED_BODY` |
| Generic/unknown | `500 INTERNAL_ERROR` | `INTERNAL_ERROR` |

### `BusinessException` codes

| Code | Meaning |
|---|---|
| `ITEM_INACTIVO` | Item is deactivated |
| `CLIENTE_INACTIVO` | Customer is deactivated |
| `PROVEEDOR_INACTIVO` | Supplier is deactivated |
| `COBRANZA_PEDIDO_INVALIDO` | Order not in a collectable state |
| `COBRANZA_REPARTIDOR_SIN_PEDIDO` | REPARTIDOR sent a collection without `pedidoId` |
| `COBRANZA_PEDIDO_NO_EN_RUTA` | `pedidoId` not in an active (non-finalized) route of the REPARTIDOR |
| `ITEM_NO_PERTENECE_AL_PEDIDO` | Substitution item not in the order |
| `PEDIDOS_EN_VIAJE` | Cannot close journey with in-transit orders |
| `LOTE_YA_DESCARTADO` | Lot already discarded |
| `AUTH_INVALIDO` | Bad credentials (login) |
| `AUTH_INACTIVO` | User deactivated |
| `PEDIDO_ESTADO_INVALIDO` | Order not in the expected state |
| `STOCK_INSUFICIENTE` | Not enough stock for the item |
| `VALIDATION_ERROR` | Bean validation failed |
| `ENTREGA_EXCEDE_RESERVA` | Delivery exceeds reserved quantity |
| `ENTREGA_VACIA` | Empty delivery |
| `MERMA_SIN_STOCK` | Merma without stock |
| `AJUSTE_REQUIERE_ADMIN` | Adjustment requires ADMIN |
| `RUTA_ESTADO_INVALIDO` | Route not in the expected state |
| `RUTA_CAPACIDAD_EXCEDIDA` | Route capacity exceeded |
| `REPARTIDOR_INVALIDO` | Invalid delivery driver |
| `CONSOLIDAR_CLIENTES_DISTINTOS` | Orders to consolidate have different customers |
| `CONSOLIDAR_PRECIOS_DISTINTOS` | Orders to consolidate have different prices |
| `OC_ESTADO_INVALIDO` | OC not in the expected state |
| `ITEM_NO_PROVISTO_POR_PROVEEDOR` | OC line references an item not supplied by the OC's supplier |
| `SIN_PENDIENTE_STOCK` | No pending-stock order to operate on |
| `MISMO_ITEM` | Same item in the operation |
| `*_DUPLICADO` | Duplicate unique value: `PROVEEDOR_CUIT`, `CLIENTE_CUIT`, `ITEM_SKU`, `USUARIO_EMAIL`, `ZONA_NOMBRE`, `CATEGORIA` |

---

## Importación CSV

La recepción de stock puede importarse en lote desde un CSV (multipart, campo `file`).
Hay dos endpoints:

| Method | Path | Contexto |
|---|---|---|
| POST | `/stock/ingresos/csv` | Ingreso **sin OC** (proveedorId opcional) |
| POST | `/ordenes-compra/{id}/recepciones/csv` | Recepción **vinculada a una OC** |

### Formato

- **Header por nombre** (la primera fila indica el nombre de cada columna).
- Separador `;` o `,` (se detecta automáticamente).
- Columnas requeridas: `sku`, `cantidad`. Opcionales: `precioUnitario`,
  `fechaVencimiento` (ISO `yyyy-MM-dd`) y `codigoLote`.

| sku | cantidad | precioUnitario | fechaVencimiento | codigoLote |
|---|---|---|---|---|
| HAR-000 | 50 | 1200.00 | 2027-01-15 | L-001 |
| ACE-006 | 20 | | | |

### Comportamiento

- **Transaccional**: o bien se importa todo, o no se persiste nada (error `400`).
- **Errores agregados por fila**: el `ApiError` lista los fallos por fila (p. ej.
  SKU inexistente, cantidad no positiva, `ITEM_NO_PROVISTO_POR_PROVEEDOR` en OC).
- En recepción con OC, `precioUnitario` se persiste en `lote.precio_unitario`
  (obligatorio). En ingreso sin OC es opcional.

---

## Example payloads

### Login

```
POST /api/auth/login
```

```json
{
  "email": "admin@pedidos.com",
  "password": "admin123"
}
```

Response `200`:
```json
{
  "token": "<jwt>",
  "email": "admin@pedidos.com",
  "roles": ["ADMINISTRATIVO"]
}
```

### Create an order

```
POST /api/pedidos
Authorization: Bearer <token>
```

```json
{
  "clienteId": 1,
  "zonaId": 1,
  "express": false,
  "items": [
    {
      "itemId": 1,
      "cantidad": 10,
      "precioUnitario": 1200.00
    }
  ],
  "observaciones": "Entregar antes de las 12"
}
```

Validation is cascading: nested `items` are validated with `@Valid` and each
entry's `cantidad` must be `@Positive`.

### Consolidate orders

```
POST /api/pedidos/consolidar
Authorization: Bearer <token>
```

```json
{
  "pedidoIds": [100, 101, 102]
}
```

Consolidation merges child orders into one new `PENDIENTE_CONFIRMACION` order;
the sources become `RECHAZADO`. All sources must share the same customer
(`CONSOLIDAR_CLIENTES_DISTINTOS`) and prices (`CONSOLIDAR_PRECIOS_DISTINTOS`).

### Discard a lot

```
POST /api/stock/lotes/15/descartar
Authorization: Bearer <token>
```

```json
{
  "motivo": "Producto dañado en estiba"
}
```

The lot is marked `DESCARTADO` and its remaining stock is booked as merma.

---

Continue to [flows](./04-flujos.md) or [security](./05-seguridad.md).
