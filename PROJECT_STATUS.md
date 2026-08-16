# Project Status — Sistema de Pedidos, Stock y Ventas

> **Fecha:** 2026-08-15. **Autor:** cierre formal de milestone + cierre de Fase D (D1-D4).
> Este documento describe el estado **verificado real** del proyecto, no aspiraciones.

## Current Milestone

**Fase D — Cimientos técnicos, resiliencia y QA (D1, D2, D3, D4) — COMPLETA y VERIFICADA**

## Status

**COMPLETA / VERIFICADA — Backend 181 tests + E2E Playwright 8 tests en verde; Fase D cerrada; Bloques P1.A, P1.B y P2.A de saneamiento aplicados y verificados.**

## Milestone Objective

Operar con 10k items, 5k clientes, 30 camiones sin perder el control: paginación/búsqueda
server-side, categorías, comboboxes, tabs con contadores, drawer + despacho en lote +
confirmación frictionada, capacidad de rutas, PWA del repartidor, y los escenarios
operativos de C8 (pedido express/alerta en ruta, faltantes + notificaciones, sustitución,
TTL de reservas, consolidación, export CSV).

## Overall Assessment

La mayoría de los flujos centrales **funcionan y son consistentes** entre backend y frontend
(ciclo de pedido, consolidación, OC/recepción, cobranzas, faltantes+notificaciones, CSV,
TTL config). Backend: **181 tests pasando, 0 fallos**. Frontend: build Next.js 16.3 OK.
Los dos bloqueantes P1 de C8 (BUG-002 autorización, BUG-003 sustitución con diferencia
negativa) fueron **resueltos y verificados en vivo**. Persisten como P1: BUG-004 (cierre de
jornada con EN_VIAJE colgados) y BUG-005 (decisión de alcance "pedido express"). Además
existe el riesgo estructural STR-001: el frontend completo está sin versionar de forma
reproducible (gitlink a commit vacío, sin submódulo, sin remote).

## Feature Status

| Feature | Implementado | Testeado | E2E | Verificado | Evidencia |
|---|---|---|---|---|---|
| Autenticación (JWT) | sí | sí | sí | sí | login 200/409, roles 403 verificados |
| C1 Paginación + búsqueda server-side | sí | sí | sí | sí | PageResponse, `?q=`, tests |
| C2 Categorías en Item + filtro | sí | sí | sí | sí | V12, campo categoria, tests |
| C3 Combobox autocomplete | sí | parcial (sin tests FE) | sí | parcial | Combobox.tsx, sin tests frontend |
| C4 Tabs con contadores en Pedidos | sí | sí | sí | sí | /pedidos/contadores, tests |
| C5 Drawer + despacho lote + friction | sí | parcial | sí | parcial | Drawer/ConfirmacionFrictionada, sin tests FE |
| C6 Capacidad de rutas | sí | sí | sí | sí | V13, validarCapacidad, tests |
| C7 PWA repartidor /turno | sí | parcial | parcial | parcial | manifest, SwipeButton, turno; sin tests FE, sin drive visual |
| C8.1 Pedido express + alerta "en ruta hoy" | parcial | parcial | parcial | parcial | alerta presente; **"express" NO implementado** (BUG-005); alerta sin acotar a hoy (QA-08) |
| C8.2 Marcar faltante/dañado + notificaciones | sí | sí | sí | sí (backend) / parcial (visual) | 200, merma, notif no-leída, /no-leidas |
| C8.3 Sustitución en destino / cierre camión | sí | sí | sí | **sí** | BUG-003 resuelto+verificado (201 dif. negativa); resta QA-09 (UI en turno) |
| C8.4 Reserva TTL 48h auto-cancel | parcial | sí (unit) | parcial | parcial | ReservaTtlJob + config + tests; auto-cancel 48h no verificable en ventana |
| C8.5 Consolidación de pedidos hijos | sí | sí | sí | sí | 201, origenes RECHAZADO (QA-12 menor: pierde fechaJornada/escala) |
| C8.6 Export CSV | sí | sí | sí | sí | 5/5 → 200 text/csv; reportes 403 para repartidor |

## Completed

* Backend F0→F6 + B1→B5 + C1→C8 + D1→D4 implementado y compilando (181 tests verdes).
* Frontend Next.js 16.3 completo (login, dashboard, pedidos, stock, clientes/items/usuarios/
  proveedores, rutas+entregas, OC, cobranzas+caja, turno) — build OK.
* Migraciones Flyway V1→V16 validadas.
* Docker backend/frontend + compose local (Podman) funcionando.
* CI (GitHub Actions) definido: backend test + frontend build + E2E Playwright (7 tests).
* **Saneamiento Bloque P1.A (integridad de dominio) aplicado y verificado:**
  - Soft delete con semántica real: item/cliente/proveedor inactivo rechazado en pedidos, OC, reservas, mermas y ajustes; capacidad `?activos=true` en comboboxes.
  - FEFO seguro: `egresarPorLotes` excluye lotes vencidos (`fechaVencimiento < hoy`).
  - Cobranza íntegra: valida `pedidoId ↔ clienteId` y estado en `{EN_VIAJE, ENTREGADO, ENTREGADO_PARCIAL}`.
- **Saneamiento Bloque P1.B (usabilidad crítica) aplicado y verificado:**
  - Carrito: cambiar categoría en NuevoPedidoForm ya no borra los items agregados (label estable vía `valueLabel`).
  - Turno operativo: PedidoCard muestra domicilio, observaciones y próximas paradas del repartidor.
  - Lotes en stock: `LoteResponse` con saldo/estado (VENCIDO/AGOTADO/VIGENTE) + `GET /stock/lotes` + sección de Lotes con filtros en `/stock`.
  - Fix de build: `tsconfig` excluye `e2e/` del typecheck (los specs no bloquean el build de la app).
- **Saneamiento Bloque P2.A (catálogo, accesos y navegabilidad) aplicado y verificado:**
  - Accesos: `/reportes/stock` abierto a `ENCARGADO_DEPOSITO`; comboboxes de creación envían `activos=true`.
  - Categoría ABMC (AUD-007): migración V17 (tabla `categoria`, `item.categoria_id`, **drop de `item.categoria`**), módulo hexagonal `categoria`, selector por `categoriaId` en item y pedidos, gestión en `/items`.
  - Dashboard accionable (AUD-011): links con pre-filtrado (`?filtro=bajo`, `?tab=lotes&filtro=vencer`, `?tab=PENDIENTE_STOCK`, `?tab=RE_AGENDADO`) + componente `Toast` global.

## Partially Completed

* C8.1 pedido "express" (solo alerta, sin flag) — BUG-005.
* C8.3 sustitución (solo diferencia positiva, solo ENTREGADO, sin UI turno) — BUG-003, QA-09.
* C8.4 TTL auto-cancel 48h (config+tests presentes; comportamiento temporal no verificado E2E).
* C7 PWA / turno (sin tests frontend, sin verificación visual).

## Known Bugs

* **BUG-002 (P1):** ~~REPARTIDOR crea ítems/clientes~~ → **RESUELTO y verificado** (403 repartidor, 201 admin).
* **BUG-003 (P1):** ~~Sustitución con diferencia negativa → 409 y revierte~~ → **RESUELTO y verificado** (201, cobranza con signo).
* **BUG-004 (P1):** Se cierra jornada con pedidos EN_VIAJE colgados. (QA)
* QA-01 (P2): rutas inexistentes/método incorrecto → 500 en vez de 404/405.
* QA-04 (P2): ~~validación anidada ausente~~ → **RESUELTO** (@Valid anidado).
* QA-05 (P2): POST /sustituciones malformado → 500.
* QA-13 (P2): ~~sustitución sin validar membresía~~ → **RESUELTO** (ITEM_NO_PERTENECE_AL_PEDIDO).

## Known Limitations

* Sin soporte offline.
* Sin monitoreo/actuator de producción (Fase D). — ✅ D3: actuator base (health/info) implementado; faltan métricas avanzadas/actuator/metrics.
* Sin tests e2e (Playwright) (Fase D). — ✅ D4: Playwright E2E implementado (7 tests: login, repartidor/turno, consolidación, sustitución, pedido express) + job CI.
* Sin tests del frontend (ningún test unit/e2e FE).
* JWT_SECRET con default inseguro en dev.
* Consultas de reportes sin optimizar para gran volumen (Fase D).

## Technical Debt

* `GlobalExceptionHandler` no mapea 404/405 → 500 (QA-01).
* Validación anidada inconsistente entre requests (`@Valid` faltante en varios).
* Frontend usa `size=500` sin paginar en turno y alertas (riesgo a gran escala).
* Frontend completo **sin versionar** de forma reproducible (STR-001).
* Sin tests frontend.
* ROADMAP.md desactualizado (109→132 tests, V1→V11→V15, C8 no reflejado).

## Architecture

Backend hexagonal (controller → port/in → service → port/out → JPA) en Java 21 + Spring Boot
4.1, Postgres 16 + Flyway (V1→V15), seguridad JWT + BCrypt. Módulos: usuario, cliente (zonas),
stock (items/lotes/ledger/FEFO/mermas/ajustes/reservas), pedido (circuito PENDIENTE_*),
ruta (despacho/asignación), compra (proveedores/OC), cobranza (remitos/cobranzas/cuenta),
sustitucion, notificacion, reporte. Frontend Next.js 16.3 + Tailwind v4 + React 19, app router,
estado local (sin lib de estado global).

## Architectural Decisions

Ver fase de decisiones en Engram (sección persistida). Resumen:
* Hexagonal Architecture (Clean-ish) con ports/adapters.
* Estados de pedido como circuito explícito `PENDIENTE_STOCK → … → ENTREGADO/RECHAZADO`.
* FEFO por lote + ledger de movimientos como única fuente de stock.
* Reservas (TTL 48h) + liberación automática vía job.
* Sustitución modelada como ingreso(original) + ajuste negativo(sustituto) + cobranza compensatoria.
* JWT stateless; BCrypt; roles por endpoint (parcial, ver BUG-002).

## Scope Deviations

### Added
* Notificaciones internas (módulo `notificacion`) — requerido por C8.2, no estaba en roadmap literal.
* Export CSV (clientes, pedidos, stock, ventas, caja) — requerido por C8.6.
* Reserva TTL job (C8a) — sub-item adicional.

### Removed
* Ninguno detectado.

### Changed
* "Pedido express" de C8.1 quedó como **alerta "en ruta hoy"** sin flag express real
  (discrepancia de alcance, BUG-005) — no intencional, requiere decisión de negocio.
* Sustitución restringida a ENTREGADO (QA-09) aunque el escenario implica "en destino"
  — restricción más estrecha que el escenario, no intencional.

## QA Status

Exploratory QA:
completed: yes (subagente + verificación manual de bloqueantes)
result: 2 bloqueantes (BUG-002, BUG-003) RESUELTOS y verificados + 13 hallazgos (QA-01..13) + STR

Automated tests:
- Backend: **181 tests, 0 fallos** (surefire).
- Frontend: **sin tests unitarios; E2E con Playwright: 7 tests verdes, estable como gate de CI** (`npm run test:e2e`).

End-to-end verification:
- Core flows vía API confirmados (login, pedido, consolidación, OC, cobranza, faltante,
  sustitución dif. positiva, ruta→entrega, CSV).
- Flujos visuales (bell, drawer, swipe, badge) verificados solo por código — **partial**.
- TTL auto-cancel 48h no verificable en ventana — **partial**.

## Backlog

Critical:
* BUG-004 (P1, cierre jornada) · BUG-005 (P1, pedido express — decisión de alcance)
* STR-001 (P1, frontend sin versionar)
* ~~BUG-002~~ · ~~BUG-003~~ — resueltos y verificados

Important:
* QA-01, QA-05, QA-09, QA-10 (P2) · QA-04, QA-13 — resueltos

Future:
* QA-08, QA-11, QA-12 (P3) · STR-002 (docs) · STR-003 (limpieza datos QA)

## Checkpoint

Commit:
- **e04061f** `checkpoint: close milestone Fase C (C1-C8)` — pusheado a `origin/main`
- frontend integrado al repo padre como carpeta normal (53 archivos; gitlink eliminado)
- **Fase D (D1-D4):** commits `048222b`, `47e71a4`, `8ad9b93`, `638e447` + **`2b3ff41` `checkpoint: close milestone Fase D (D1-D4)`** — pusheado a `origin/main`
- working tree limpio (verificado en auditoría de cierre)

## Next Milestone

Continuación de Fase D: monitoreo avanzado (actuator/metrics), API versioning, más cobertura E2E,
optimización de reportes, multi-depósito/empresa, integraciones externas (AFIP, pasarelas).

## Recommended Next Action

1. Refinamiento UX/UI o hardening de producción/Docker/CI-CD (ver opciones del handoff de cierre).
2. Ordenar la cola de confirmación del backend (STR-004) — mejora de determinismo.
3. Monitoreo avanzado: exponer `/actuator/metrics` y métricas de negocio.
4. Endpoints de limpieza/reset para tests E2E (STR-005).
5. Limpiar datos de QA en BD local (STR-003).

## Notes for Future Agents

* El repo padre ahora tiene 3 commits; el checkpoint `e04061f` cierra Fase C e integra el
  frontend como carpeta normal (antes era un gitlink a un commit vacío, sin submódulo ni remote).
* El trabajo de Fase A está en los 2 primeros commits; Fase B y C en el checkpoint.
* Los tests pasan contra Postgres local (`jdbc:postgresql://localhost:5432/pedidos`).
* Aplicar los criterios de aceptación de BACKLOG.md; no usar Engram como backlog.
