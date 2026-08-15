# Roadmap — Sistema de Pedidos, Stock y Ventas

> Priorización acordada (2026-08): **Fase A completa** ✅ · **Fase B completa** ✅ ·
> **Fase C completa** ✅ · **Fase D EN EJECUCIÓN** (cimientos técnicos, resiliencia y QA).

## Estado actual

- **Backend** completo (F0→F6 + B1→B5): `usuario`, `cliente` (zonas), `stock`
  (items, lotes, ledger, FEFO por lote, mermas, ajustes con umbral, reservas),
  `pedido` (circuito con `PENDIENTE_STOCK`, pedido hijo), `ruta` (despacho +
  asignación), `compra` (proveedores + OC con recepción → stock), `cobranza`
  (remitos + cobranzas + cuenta de cliente), `reporte` (stock/ventas/rutas/caja),
  `sustitucion`, `notificacion`, seguridad JWT + BCrypt, auditoría, springdoc, actuator (health/info). **148 tests**.
- **Frontend** completo: login, Dashboard (alertas: stock bajo, pendientes,
  re-agendados, lotes por vencer), Pedidos (cliente+zona, acciones por rol),
  Stock (operativo), Clientes/Items/Usuarios/Proveedores (ABMC), Rutas +
  Entregas, Órdenes de Compra, Cobranzas + Caja. Next.js 16 + Tailwind v4,
  auditado con Impeccable.
- Migraciones Flyway V1→V16 · Dockerfile backend/frontend + compose · CI en
  GitHub Actions · App local en containers Podman (`:8080/api` + `:3000`).
- Observabilidad base (D3): `/actuator/health` público (con liveness/readiness y detalles DB por rol), `/actuator/info` solo `ADMINISTRATIVO`.

---

## Fase A — Cerrar el dominio ✅ COMPLETA

A1 Rutas+Entregas · A2 Stock operativo · A3 Cambio de contraseña · A4 Reactivar
items · A5 Alertas · A6 Refinamientos (paginación/@Valid/ES) · A7 Calidad
(integración + CI) · A8 Operación (Docker + JWT real).

---

## Fase B — Modelo comercial ✅ COMPLETA

B1 Cobranzas + Remitos · B2 Proveedores + OC (+ alerta 3.4) · B3 Precios de
lista · B4 FEFO/FIFO + cajón roto + umbral de ajustes · B5 Resumen de caja.

---

## Fase C — Usabilidad y experiencia a gran escala ✅ COMPLETA

Objetivo: operar con 10k items, 5k clientes, 30 camiones sin perder el control.
Checkpoint de cierre: `e04061f`. C8 completo (pedido express + alerta en ruta, faltante +
notificaciones, sustitución, TTL reservas, consolidación, export CSV). 2 bloqueantes
resueltos (autorización por rol, sustitución con diferencia negativa).

| # | Item | Notas |
|---|---|---|
| C1 | **Backend: paginar + búsqueda server-side (`?q=`)** en `/items`, `/clientes`, `/usuarios`, `/proveedores`, `/movimientos` | PREREQUISITO de todo lo demás: sin esto el autocomplete no puede existir |
| C2 | **Categorías en Item** + selector agrupado/buscable | Distinguir "Harina 000" de "Harina 000 Premium" |
| C3 | **Combobox autocomplete** (search-as-you-type con debounce) en clientes/items/repartidor | Prohibir `<select>` planos para entidades principales |
| C4 | **Tabs con contadores por estado** en Pedidos (exception-based UI) + vistas guardadas | `[140] En preparación` · `[12] Sin stock` · `[5] Re-agendados` |
| C5 | **Drawer lateral** (contexto visible) + **despacho en lote** + **confirmación frictionada** | "Escriba CANCELAR" para acciones masivas |
| C6 | **Capacidad en rutas** + matriz de asignación con batch actions | Split-screen zonas ↔ camiones con indicador de capacidad |
| C7 | **PWA del repartidor one-thumb** | Modo turno, swipe para entregar, "entregar todo menos N" |
| C8 | **Escenarios operativos**: pedido express + alerta en ruta · merma desde preparación + notificación admin · sustitución en destino/cierre de camión · TTL de reservas (48hs) · consolidación de hijos · exportación CSV/PDF | Surgen de la revisión de escenarios pre-B |

---

## Fase D — Cimientos técnicos, resiliencia y QA (EN EJECUCIÓN)

CI/CD completo, monitoreo (actuator/métricas), API versioning, tests e2e
(Playwright), índices/consultas de reportes optimizadas, multi-depósito /
multi-empresa, integraciones externas (AFIP, pasarelas de pago).

### D1 — Resiliencia y errores ✅ COMPLETA
- BUG-004: cerrar jornada se bloquea si hay pedidos `EN_VIAJE` (409 + números listados).
- QA-01/QA-05: `GlobalExceptionHandler` mapea 404/405/415; `SustituirRequest` validado (`@NotNull`/`@Positive` + `@Valid`).

### D2 — Pedido Express ✅ COMPLETA
- Flag `express` (default false) en Pedido (V16), prioridad en cola de preparación/despacho
  (`express DESC, fecha_creacion ASC`), badge Express en UI y checkbox al crear.

### D3 — Integridad de datos (P2) + Observabilidad base ✅ COMPLETA
- QA-04: validación en cascada `@Valid` en `CrearPedidoRequest.items` y `EntregaRequest.entregas`; `cantidadEntregada` `@Positive`.
- QA-13: sustitución valida que `itemOriginalId` pertenezca al pedido (`ITEM_NO_PERTENECE_AL_PEDIDO`) antes de tocar stock.
- Actuator: `spring-boot-starter-actuator`; `/actuator/health` (y probes liveness/readiness) público, `/actuator/info` y resto `/actuator/**` solo `ADMINISTRATIVO`; `show-details: when-authorized`; metadata `info.app`.
