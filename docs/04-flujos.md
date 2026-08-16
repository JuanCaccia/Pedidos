# End-to-End Flows by Role

> Each role operates a slice of the business. This document walks the complete
> flow for each actor, tying screens to backend endpoints (see
> [03-api.md](./03-api.md)) and domain rules (see
> [02-modelo-datos.md](./02-modelo-datos.md)).

---

## VENDEDOR — Sales

### Create an order

1. **Dashboard**: the seller lands on a dashboard with actionable alerts
   (low stock, pending orders, rescheduled orders, expiring lots).
2. **Pedidos screen**: pick a customer (and its zone) and add items. Express
   checkbox optionally flags the order for priority.
3. Frontend calls `POST /pedidos`, which validates the customer, items and
   quantities.
4. The order is created in `PENDIENTE_CONFIRMACION`. The seller confirms it
   with `POST /pedidos/{id}/confirmar`.
   - If every item has stock → `PENDIENTE_PREPARACION`.
   - If any item lacks stock → `PENDIENTE_STOCK`.
5. The order now flows to the warehouse queue.

### Filter items by category

On the **Items** selector the seller searches server-side (`?q=`) and can
group/filter by **category** (`GET /items`), distinguishing products with
similar names (e.g. "Harina 000" vs. "Harina 000 Premium").

### Manage customers

The seller creates and updates customers (`POST /clientes`, `PUT /clientes/{id}`)
and records collections (`POST /cobranzas`) once orders reach an eligible state.

---

## ENCARGADO_DEPOSITO — Warehouse

### Purchase-order receipt → stock

1. Create/update suppliers and purchase orders (`POST /proveedores`,
   `POST /ordenes-compra`).
2. When goods arrive, receive the OC (`POST /ordenes-compra/{id}/recepciones`).
   Receiving can be partial (`RECIBIDA_PARCIAL`) or full (`RECIBIDA`) and feeds
   stock with `INGRESO` movements.
3. Lots created from receipt carry provenance (`lote.proveedor_id`).

### Stock operations

On the **Stock** screen:
- **Ingreso**: `POST /stock/ingresos` adds a lot with an `INGRESO` movement.
- **Merma**: `POST /stock/mermas` books wastage (`MERMA` movement).
- **Ajuste**: `POST /stock/ajustes` corrects inventory (`AJUSTE_INVENTARIO`,
  requires ADMIN).
- **Descartar lote**: `POST /stock/lotes/{id}/descartar` marks a lot
  `DESCARTADO` and books the remainder as merma.
- View per-item stock, movements and lots; expiring-soon lots via
  `GET /stock/lotes/por-vencer?dias=30`.

### Dispatch

1. On the **Pedidos** dashboard the warehouse acts on `PENDIENTE_STOCK` and
   `PENDIENTE_PREPARACION` queues.
2. To satisfy a stock shortage: `POST /pedidos/{id}/agregar-stock` or mark the
   item as missing with `POST /pedidos/{id}/marcar-faltante`.
3. Once prepared, `POST /pedidos/{id}/despachar` moves the order to
   `PENDIENTE_ENTREGA` for route assignment.

---

## REPARTIDOR — Delivery driver

### Route shift

1. View and manage routes (`GET /rutas`): routes are planned by zone with a
   capacity (`ruta.capacidad_bultos`).
2. Assign orders to a route (`POST /rutas/{id}/pedidos`) within capacity
   (`RUTA_CAPACIDAD_EXCEDIDA` otherwise).
3. Start the route (`POST /rutas/{id}/iniciar`) → orders become `EN_VIAJE`.
4. Deliver: `POST /pedidos/{id}/entregas` records per-item delivered
   quantities and generates the remit (remito).
   - Full delivery → `ENTREGADO`.
   - Partial delivery → `ENTREGADO_PARCIAL` + a child order for the remainder.
5. Close the route (`POST /rutas/{id}/cerrar`); this fails with
   `PEDIDOS_EN_VIAJE` if any order is still in transit.

### Substitution at destination

If a product is missing/unsuitable at delivery, `POST /sustituciones` replaces
the original item with a substitute and records the price difference
(`diferencia_precio`). A negative difference produces a compensating
collection (e.g. a €/$ 250 adjustment).

---

## ADMINISTRATIVO — Administration

1. **Users & roles**: manage users (`POST /usuarios`, roles, activation,
   password reset) — `ADMINISTRATIVO` only.
2. **Actionable dashboard**: counters by order state
   (`GET /pedidos/contadores`) with exception-based UI — e.g. `[140] En
   preparación`, `[12] Sin stock`, `[5] Re-agendados`.
3. **Reports** (`/reportes/**`): stock, sales, routes and cash summaries,
   each exportable to CSV.
4. **Consolidation**: merge pending child orders with
   `POST /pedidos/consolidar` to create a single new order.
5. **Notifications**: review in-app alerts (`GET /notificaciones`).
6. **Observability**: `GET /actuator/info` app metadata.

---

Continue to [security](./05-seguridad.md) or [tests](./06-tests.md).
