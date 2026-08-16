# Pedidos

> Sales, stock and delivery management for a distributor: orders, lot-based
> inventory (FEFO), routes, deliveries, collections, purchasing, reports and
> role-based access — from sale to cash collected.

**Backend:** Spring Boot 4.1.0 · Java 21 · PostgreSQL (Flyway V1→V19) · JWT.
**Frontend:** Next.js 16 · React 19.
**Quality:** 198 backend tests + 8 Playwright E2E, CI on GitHub Actions.

---

## Run locally

```bash
cd backend
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui/ |

Seed users: `admin@pedidos.com` / `admin123` (all roles) and
`repartidor@pedidos.com` / `repartidor123`.

See [docs/00-overview.md](./docs/00-overview.md) for details.

---

## Repository layout

```
backend/
  docker-compose.yml      # db + backend + frontend
  pedidos/                # Spring Boot application (src/main/...)
frontend/
  frontend/               # Next.js application
.github/workflows/ci.yml  # backend tests, frontend build, E2E
docs/                     # this documentation
ROADMAP.md                # product roadmap (Spanish)
BACKLOG.md                # backlog (Spanish)
PROJECT_STATUS.md         # status (Spanish)
```

---

## Documentation

| Doc | Content |
|---|---|
| [docs/08-onboarding.md](./docs/08-onboarding.md) | **Start here** — recommended reading path, condensed route, command & seed cheatsheet |
| [docs/00-overview.md](./docs/00-overview.md) | System summary, domains, roles, stack, local run |
| [docs/01-arquitectura.md](./docs/01-arquitectura.md) | Hexagonal architecture, ports & adapters, key flows |
| [docs/02-modelo-datos.md](./docs/02-modelo-datos.md) | Data model (V1→V19), entity relationship diagram (Mermaid), enums, lifecycles, soft-delete |
| [docs/03-api.md](./docs/03-api.md) | REST endpoints, authorization, errors, payloads |
| [docs/04-flujos.md](./docs/04-flujos.md) | End-to-end flows per role |
| [docs/05-seguridad.md](./docs/05-seguridad.md) | JWT auth and role/route matrix |
| [docs/06-tests.md](./docs/06-tests.md) | Running tests, coverage, E2E, CI |
| [docs/07-convenciones.md](./docs/07-convenciones.md) | Validation, errors, FEFO, auditing, conventions |
