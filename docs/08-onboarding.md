# Onboarding — Recommended Reading Path

> New to the repo? Start here. This guide tells you **what to read, in what
> order, how long it should take**, and *why* each document matters. Follow the
> full path for a solid mental model, or use the
> [condensed route](#condensed-route-little-time) if you only have a few minutes.

---

## Purpose

`docs/` is written in technical English and assumes some familiarity with
Spring Boot, Next.js and PostgreSQL — but **no prior knowledge of this
codebase**. The documents form a dependency chain: each one builds on the
concepts introduced by the previous ones. Reading them out of order works, but
you will spend more time filling gaps on your own.

This guide is the entry point. Use it as a checklist: tick off each step as you
finish it. When you reach the end you should be able to open the code and
answer *"where is this handled and why?"* without guessing.

---

## Recommended reading path

| # | Doc | Time | Why it matters |
|---|---|---|---|
| 1 | [README.md](../README.md) | 5 min | One-screen orientation: what the system does, stack, how to run it, seed users. Anchors every later doc to something concrete. |
| 2 | [docs/00-overview.md](./00-overview.md) | 15 min | The **domain**: what a distributor does day-to-day, the four roles, the sale→stock→route→delivery→cash loop, and how to run each service. Everything else explains *parts* of this story. |
| 3 | [docs/02-modelo-datos.md](./02-modelo-datos.md) | 20 min | The **data model**: entities, states, enums, lifecycles and soft-delete. Start from the Mermaid relationship diagram, then read the per-table detail. This is your map of the business facts. |
| 4 | [docs/01-arquitectura.md](./01-arquitectura.md) | 20 min | **How the code is organized**: hexagonal architecture, ports & adapters, layers and modules, with worked example flows. Maps the domain onto actual classes and packages. |
| 5 | [docs/05-seguridad.md](./05-seguridad.md) | 10 min | **AuthN/AuthZ**: JWT flow, roles and the role/route matrix. Needed before you touch any protected endpoint. |
| 6 | [docs/04-flujos.md](./04-flujos.md) | 15 min | **End-to-end flows per role**, from UI through API to database. Reinforces how the pieces connect in a real journey. |
| 7 | [docs/03-api.md](./03-api.md) | on demand | A **reference**, not a read-through. Browse it when you need an endpoint, payload or error. Do not read front-to-back. |
| 8 | [docs/06-tests.md](./06-tests.md) + [docs/07-convenciones.md](./07-convenciones.md) | before writing code | **How we prove and style work**: running tests, coverage, E2E/CI, plus validation, error handling, FEFO and auditing conventions. Read both *before* your first change. |

### Why this order

Each step unlocks the next:

1. **README** gives you the vocabulary and the run commands.
2. **00-overview** gives you the *domain story* — the why behind every table.
3. **02-modelo-datos** gives you the *facts* the system stores.
4. **01-arquitectura** gives you *where* those facts live in code.
5. **05-seguridad** tells you *who can touch what*.
6. **04-flujos** shows a whole journey end to end, tying code to behavior.
7. **03-api** is there to look things up, not memorize.
8. **06 + 07** are the rules of the road before you write anything.

---

## Condensed route (little time)

If you only have ~60 minutes, the high-value core is:

```
README.md  →  docs/00-overview.md  →  docs/02-modelo-datos.md  →  docs/01-arquitectura.md
```

That gives you the domain, the data, and the code structure — enough to be
productive and to know *where* to go back for the rest.

---

## Cheatsheet: commands & boot data

| Task | Command |
|---|---|
| Run everything (db + backend + frontend) | `cd backend && docker compose up --build` |
| Run backend alone (Spring Boot) | `cd backend/pedidos && ./mvnw spring-boot:run` |
| Run frontend alone (Next.js) | `cd frontend/frontend && npm run dev` |
| Backend tests | `cd backend/pedidos && ./mvnw test` |
| E2E tests (Playwright) | `npm run test:e2e` |

| Service / detail | Value |
|---|---|
| Frontend base URL | http://localhost:3000 |
| Backend base URL | http://localhost:8080 |
| API context path | `/api` (e.g. http://localhost:8080/api) |
| Swagger / OpenAPI JSON | `/v3/api-docs` |
| Swagger UI | `/swagger-ui` |

Seed users:

| User | Password | Roles |
|---|---|---|
| `admin@pedidos.com` | `admin123` | all roles |
| `repartidor@pedidos.com` | `repartidor123` | `REPARTIDOR` |

---

## Where to look next

- **Need a specific endpoint or payload?** → [docs/03-api.md](./03-api.md).
- **About to write your first change?** → read
  [docs/06-tests.md](./06-tests.md) and [docs/07-convenciones.md](./07-convenciones.md) first.
- **Deeper on a domain area** (stock/FEFO, routes, purchasing, cash)? → revisit the
  relevant domain rows in [docs/00-overview.md](./00-overview.md) and trace them
  through [docs/04-flujos.md](./04-flujos.md).
- **Want the bird's-eye view of the schema again?** → the Mermaid diagram at the
  top of [docs/02-modelo-datos.md](./02-modelo-datos.md).
