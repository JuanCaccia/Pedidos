# Conventions

> These are the coding and design conventions used across the backend. They
> keep the codebase predictable, consistent and testable.

---

## Cascading validation

Validation is applied **in cascade** using Bean Validation on DTO records:

- Request DTOs are records with `@NotEmpty`/`@Positive`/etc. on fields.
- Nested collections (e.g. `items` in a create-order request) are annotated
  with `@Valid`, so each element is validated in turn.
- Failures surface as `VALIDATION_ERROR` with `fieldErrors` detailing each
  field (see [03-api.md](./03-api.md)).

## Centralized error handling

- Every `@RestController` error goes through a single `GlobalExceptionHandler`.
- `BusinessException(code, message)` carries a machine-readable code mapped to
  `409 CONFLICT`.
- Response shape is always `ApiError { timestamp, status, code, message, fieldErrors }`.
- HTTP mapping: 404 (not found / resource not found), 405 (method), 415
  (media type), 400 (validation / malformed body), 500 (internal).

## Soft-delete with business rules

- Tables with `activo` (`zona`, `usuario`, `cliente`, `item`, `proveedor`,
  `categoria`) are never physically deleted.
- Deactivating an entity is guarded by business rules so invalid operations are
  rejected: `ITEM_INACTIVO`, `CLIENTE_INACTIVO`, `PROVEEDOR_INACTIVO`,
  `AUTH_INACTIVO`.

## Collectable states

A collection (`cobranza`) may only be recorded against an order in one of:
`EN_VIAJE`, `ENTREGADO`, `ENTREGADO_PARCIAL`. Anything else triggers
`COBRANZA_PEDIDO_INVALIDO`.

## FEFO lot selection

- Stock consumption follows **FEFO** (first-expiring-first-out).
- FEFO **excludes** `DESCARTADO` lots and expired lots; it sorts candidates by
  `fecha_vencimiento`, then `fecha_ingreso`, then `id`.
- Expired lots are derived at read time (no DB write on expiry).

## `disponibleDeLote`

- Available quantity per lot accounts for `AJUSTE_INVENTARIO`:
  `ingresos + ajustes − egresos − mermas` per lot.
- A signed adjustment may target either an **item** or a **lot**.

## Clean category cut (no legacy)

The `categoria` migration (`V17`) creates the normalized table, backfills
`item.categoria_id` from the legacy free-text `item.categoria` column, and then
**drops** the legacy column. There is no residual legacy text category anywhere.

## Auditing

- `BaseEntity` provides `@CreatedDate`, `@LastModifiedDate` and `@CreatedBy`.
- The auditor is taken from the `SecurityContext`; the fallback value is
  `"system"`.

## Transactionality

- Service classes are annotated `@Transactional(readOnly = true)` at class
  level (read-only default).
- Mutating methods override it with `@Transactional` (read-write) so each
  write operation runs in its own transaction.

---

This is the last document. Start again at the [overview](./00-overview.md).
