# Architecture — Hexagonal (Ports & Adapters)

> The backend follows **hexagonal architecture** (also called Ports &
> Adapters). Business logic lives at the center and depends only on ports
> (interfaces); infrastructure details (web, persistence, cross-module calls)
> are pushed to the edges as adapters.

All Java packages live under `com.sistema.*`.

---

## Layers

| Layer | Package | Purpose |
|---|---|---|
| **Use cases (inbound ports)** | `<modulo>.port.in` | Interfaces that express *what the application can do* (e.g. `CrearPedido`, `ConfirmarPedido`). Named as verbs. |
| **Outbound ports** | `<modulo>.port.out` | Interfaces the domain needs from the outside: repositories, plus **gateways** to other modules. |
| **Services (application core)** | `<modulo>.service` | Implementations of the use cases. Pure business rules, no Spring MVC or JPA types leaked in. |
| **Inbound adapters** | `<modulo>.adapter.in.web` | REST controllers + DTOs (request/response records) + validation. |
| **Persistence adapters** | `<modulo>.adapter.out.persistence` | JPA entities, Spring Data repositories, mappers and repository adapters that implement the outbound ports. |
| **Cross-module gateways** | `<modulo>.adapter.out.<otromodulo>` | Adapters that implement an outbound port *of another module* so modules can collaborate without depending on each other's internals. |
| **Security & config** | `com.sistema.security`, `com.sistema.config` | JWT filter/security config and `DataSeeder`. |

---

## Module layout

Each functional domain is a self-contained module. The example shows the
`pedido` module:

```
com.sistema.pedido
├── port
│   ├── in/          # CrearPedido, ConfirmarPedido, GestionarEntrega, ReAgendarPedido, ...
│   └── out/         # PedidoRepository, PedidoItemRepository, StockGateway, RemitoGateway
├── service
│   └── PedidoService            # implements all pedido use cases
└── adapter
    ├── in/web/      # PedidoController + DTOs
    ├── out/persistence/  # PedidoEntity, PedidoItemEntity, mappers, adapters
    └── out/stock/   # StockGatewayImpl (implements pedido's StockGateway port)
```

Domains present: `usuario`, `cliente`, `stock`, `pedido`, `ruta`, `compra`,
`cobranza`, `sustitucion`, `notificacion`, `reporte`, `categoria`, `common`,
`security`, `config`.

### Ports & adapters pattern

- **`port/in`** are the use-case interfaces — the "verbs" of the system. They
  are what controllers call.
- **`port/out`** contains two kinds of contracts:
  - **Repositories** to persist the module's own aggregates.
  - **Gateways** that the module needs from *other* modules. Each gateway is
    implemented by an adapter living in the other module (e.g. `PedidoService`
    calls a `StockGateway`; the actual implementation `StockGatewayImpl` lives
    in `stock.adapter.out.pedido`).
- **`service`** classes implement the use cases and orchestrate both
  repositories and gateways. This is where transactional, domain rules live.

---

## Example flows (with code)

### 1. Confirm an order (with stock reservation)

When an order is confirmed, the system checks and reserves stock through a
gateway to the `stock` module.

```
PedidoController.confirmar
  └─ ConfirmarPedido (use case)
       └─ PedidoService.confirmarPedido
            └─ StockGateway.reservar(...)        # port/out (pedido)
                 └─ stock/adapter/out/pedido/StockGatewayImpl   # actual impl
                      ├─ validates ITEM_INACTIVO / STOCK_INSUFICIENTE
                      └─ creates RESERVA_PEDIDO movement
```

### 2. Discard a lot

A warehouse operation that marks a lot as discarded and books the remaining
stock as merma.

```
StockController.descartarLote
  └─ DescartarLote (use case)
       └─ StockService.descartar(...)
            └─ LoteRepository
```

### 3. Record a delivery

Recording a delivery moves stock out, releases any residual reservation and
generates the delivery receipt (remito).

```
PedidoController.registrarEntrega
  └─ GestionarEntrega (use case)
       └─ PedidoService.registrarEntrega
            ├─ StockGateway.egresar(...)          # EGRESO_VENTA movement
            ├─ StockGateway.liberarReserva(...)   # release leftovers
            └─ RemitoGateway.generarRemito(...)   # create remito + lines
```

---

## Key architectural decisions

- **Hexagonal over layered**: domain logic does not depend on frameworks or
  persistence; new infrastructure (a different DB, an external API) can be
  added by writing a new adapter without touching the core.
- **Gateways decouple modules**: modules collaborate through `port/out`
  interfaces implemented elsewhere, so the `pedido` module never imports
  `stock` internals directly.
- **Stateless, DTO-based API**: controllers exchange records (DTOs) with the
  service layer; entities are never exposed over HTTP.
- **Service-level transactions**: services are annotated
  `@Transactional(readOnly=true)` at class level with read-write
  `@Transactional` overridden on mutating methods.
- **Single `GlobalExceptionHandler`** centralizes HTTP error mapping (see
  [03-api.md](./03-api.md)).

---

Continue to the [data model](./02-modelo-datos.md) or the [API reference](./03-api.md).
