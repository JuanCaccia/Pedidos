# Pedidos — System Overview

> **Pedidos** is a full sales, stock and delivery management system for a
> distributor that sells goods to retail clients and operates a fleet of
> delivery routes. It is a monorepo with a Spring Boot backend, a Next.js
> frontend, a PostgreSQL database and a CI/CD pipeline.

This document is the entry point for anyone — developer or technical
stakeholder — who needs to understand the product without reading code. It
summarizes the domains covered, the user roles, the core business flow, the
technology stack and how to run the project locally.

---

## What the system does

Pedidos covers the entire commercial cycle of a distributor:

| Domain | Responsibility |
|---|---|
| **Clientes (Customers)** | Master data for customers, grouped by **zones** for route planning. |
| **Items & Categorías** | The catalog of sellable products, with unit of measure, list price, minimum stock and an optional category. |
| **Stock (Inventory)** | Stock managed by **lots** (FEFO), a movement ledger, reservations, wastage (merma), manual adjustments and supplier receipt. |
| **Pedidos (Orders)** | Orders with a full state machine, express priority, stock reservation, shortage handling and consolidation of partial deliveries. |
| **Rutas & Entregas (Routes & Deliveries)** | Route planning by zone, capacity control, dispatch, in-transit tracking and delivery with per-item quantities. |
| **Compras (Purchasing)** | Suppliers and purchase orders (OC) with partial receipt that feeds stock. |
| **Cobranza & Remitos (Collections & Remits)** | Delivery receipts (remitos), cash collection with payment methods, and per-customer account balance. |
| **Sustitución (Substitution)** | Replacing an item at delivery time, with price-difference settlement. |
| **Reportes (Reports)** | Stock, sales, routes and cash summaries, exportable to CSV. |
| **Notificaciones (Notifications)** | In-app alerts targeted at specific user roles. |
| **Seguridad (Security)** | JWT authentication with role-based authorization. |

---

## Roles

Four roles control what each user can do. Users may hold multiple roles.

| Role | Description |
|---|---|
| `VENDEDOR` | Salesperson: creates orders and manages customers. |
| `ENCARGADO_DEPOSITO` | Warehouse manager: stock operations, purchase orders, dispatch, categories, items. |
| `REPARTIDOR` | Delivery driver: runs routes, records deliveries and substitutions. |
| `ADMINISTRATIVO` | Administrator: users, roles, reports, cash, everything. |

---

## Core business flow

The main operational loop, from a sale to the cash collected:

```
Venta ──> Stock ──> Ruta ──> Entrega ──> Cobranza
 └─ order      └─ reserve    └─ plan    └─ deliver+remit └─ collect
```

1. **Venta**: a `VENDEDOR` creates an order for a customer in a zone. The order
   reserves stock for each item.
2. **Stock**: if stock is available the order moves to preparation; if any item
   lacks stock it goes to `PENDIENTE_STOCK` until the warehouse adds stock or
   the order is rejected/consolidated.
3. **Ruta**: the warehouse dispatches orders to routes and the `REPARTIDOR`
   starts the route.
4. **Entrega**: the driver records delivered quantities; a remit (remito) is
   generated and partial deliveries spawn child orders for the remainder.
5. **Cobranza**: once delivered (or in-transit for eligible states), a
   collection is recorded against the customer.

Express orders are prioritized in the preparation/dispatch queue. Orders left
inactive for 48 hours are auto-cancelled by a background job.

---

## Technology stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.1.0 · Java 21 · Maven (`com.jmc:pedidos`) |
| Backend deps | Spring Data JPA, Flyway, WebMVC, Security, Validation, Actuator, springdoc-openapi 3.1.0, jjwt 0.13.0, PostgreSQL driver, Lombok |
| Database | PostgreSQL (Flyway migrations V1→V22) |
| Frontend | Next.js 16.3.0 · React 19.2.8 · ESLint · Playwright (E2E) |
| Containerization | Docker Compose (`db`, `backend`, `frontend`) |
| CI | GitHub Actions (backend tests, frontend build, E2E) |

---

## Running locally

The backend ships a `docker-compose.yml` that boots the database, the backend
and the frontend together (Podman/Docker compatible).

```bash
docker compose up --build
```

Endpoints after startup:

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui/ |
| Health | http://localhost:8080/api/health |

### Seed users

On first boot, `DataSeeder` creates the following accounts (do not use in
production):

| Email | Password | Roles |
|---|---|---|
| `admin@pedidos.com` | `admin123` | `ADMINISTRATIVO`, `VENDEDOR`, `ENCARGADO_DEPOSITO`, `REPARTIDOR` |
| `repartidor@pedidos.com` | `repartidor123` | `REPARTIDOR` |

It also seeds a couple of zones, customers and items with an initial stock lot.

---

## Documentation index

| Doc | Content |
|---|---|
| [00-overview.md](./00-overview.md) | This document — system summary, stack, local run. |
| [01-arquitectura.md](./01-arquitectura.md) | Hexagonal architecture, package layout, ports/adapters, key flows. |
| [02-modelo-datos.md](./02-modelo-datos.md) | Data model (migrations V1→V22), enums, entity lifecycles, soft-delete. |
| [03-api.md](./03-api.md) | REST endpoints by module, authorization, error format, payloads. |
| [04-flujos.md](./04-flujos.md) | End-to-end flows per role. |
| [05-seguridad.md](./05-seguridad.md) | JWT authentication and the role/route authorization matrix. |
| [06-tests.md](./06-tests.md) | How to run tests, coverage, E2E, CI jobs. |
| [07-convenciones.md](./07-convenciones.md) | Code conventions: validation, errors, soft-delete, FEFO, auditing. |

See the repo root `README.md` for a short project card.
