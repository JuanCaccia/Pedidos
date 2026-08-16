# Tests

> The project has automated coverage at three levels: backend unit tests,
> backend integration tests, and frontend end-to-end (Playwright) tests. CI
> runs all three on every push to `main` and on pull requests.

---

## Backend

### Run

```bash
./mvnw test
```

Runs from `backend/pedidos`. This executes **204 tests across 19 files**.

### Unit tests (Mockito, per service)

| Test class | Tests | Covers |
|---|---|---|
| `PedidoServiceTest` | 33 | Order state machine, stock reservation, consolidation, dispatch, delivery, deterministic confirmation queue ordering |
| `StockServiceTest` | 50 | Ingresos, mermas, ajustes, lotes, FEFO, reservations, discard |
| `RutaServiceTest` | 15 | Route planning, capacity, start/close |
| `OrdenCompraServiceTest` | 13 | OC lifecycle, partial/full receipt |
| `CategoriaServiceTest` | 11 | Category CRUD, soft-delete |
| `CobranzaServiceTest` | 9 | Collection rules, customer account |
| `UsuarioServiceTest` | 9 | User CRUD, roles, password |
| `ClienteServiceTest` | 8 | Customer CRUD, CUIT, zones |
| `ProveedorServiceTest` | 6 | Supplier CRUD, CUIT |
| `NotificacionServiceTest` | 5 | Notifications, read/unread |
| `ReporteServiceTest` | 5 | Stock/sales/routes/cash reports |
| `SustitucionServiceTest` | 4 | Substitution, price difference |
| `AuthServiceTest` | 4 | Login, token generation |
| `JwtServiceTest` | 4 | JWT signing, prod `JWT_SECRET` fail-fast (null/blank/default) |
| `ZonaServiceTest` | 3 | Zone CRUD |
| `RemitoServiceTest` | 1 | Remit generation |

### Integration tests (MockMvc + Postgres)

| Test class | Tests | Covers |
|---|---|---|
| `PedidosIntegrationTest` | 22 | JWT login, full order flow, role permissions, inactive item, lot discard, expired lot, actuator info requires admin, category requires deposit |
| `TestResetNoExisteEnProdTest` | 1 | `/test/reset` NOT registered in `prod` profile |
| `PedidosApplicationTests` | 1 | Spring context load |

---

## Frontend E2E (Playwright)

### Run

```bash
npm run test:e2e
```

`playwright.config.ts` uses `baseURL http://localhost:3000` and `1 worker`.
A `globalSetup` (`e2e/global-setup.ts`) calls `POST /api/test/reset` before the
suite so the database starts clean and reproducible each run.
Credentials come from `e2e/helpers.ts` (ADMIN and REPARTIDOR).

**8 tests across 6 spec files:**

| Spec | Tests | Covers |
|---|---|---|
| `auth.spec.ts` | 3 | Valid login, invalid login, repartidor navigation |
| `dashboard.spec.ts` | 1 | "Lotes por vencer" link → `/stock` pre-filtered |
| `pedidos.spec.ts` | 1 | Create express order |
| `consolidacion.spec.ts` | 1 | Consolidate 2 orders |
| `repartidor.spec.ts` | 1 | Mark `ENTREGADO` in a shift |
| `sustitucion.spec.ts` | 1 | Substitute item + compensating collection 250 |

---

## CI (GitHub Actions — `.github/workflows/ci.yml`)

Triggered on push to `main` and on pull requests. Three jobs:

| Job | Steps |
|---|---|
| **backend** | Postgres 16-alpine service + Java 21 (Temurin) + `./mvnw -q test` |
| **frontend** | Node 22 + `npm ci` + `npm run build` |
| **e2e** | Boots backend (`spring-boot:run`, waits `/api/health`) + frontend (waits `/login`) + `npm run test:e2e` |

---

Continue to [conventions](./07-convenciones.md).
