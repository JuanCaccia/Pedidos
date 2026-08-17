# Project Status — Sistema de Pedidos, Stock y Ventas

> **Fecha:** 2026-08-17. **Autor:** cierre formal de milestone + cierre de Fase D (D1-D4) + cierre de Hardening de Producción + 4 bloques de mejoras + reestructuración de roles y UI (Etapas 1 y 2).
> Este documento describe el estado **verificado real** del proyecto, no aspiraciones.

## Current Milestone

**Hardening de Producción — COMPLETO y VERIFICADO**

**Fase D — Cimientos técnicos, resiliencia y QA (D1, D2, D3, D4) — COMPLETA y VERIFICADA**

## Status

**COMPLETA / VERIFICADA — Backend 265 tests + E2E Playwright 8 tests en verde; Hardening de Producción cerrado; Fase D cerrada; Bloques P1.A, P1.B, P2.A y P2.B de saneamiento aplicados y verificados; 4 bloques de mejoras de compra/stock aplicados y verificados; reestructuración de roles (Etapa 1) y UI por rol (Etapa 2) aplicadas y verificadas.**

## Milestone Objective

Operar con 10k items, 5k clientes, 30 camiones sin perder el control: paginación/búsqueda
server-side, categorías, comboboxes, tabs con contadores, drawer + despacho en lote +
confirmación frictionada, capacidad de rutas, PWA del repartidor, y los escenarios
operativos de C8 (pedido express/alerta en ruta, faltantes + notificaciones, sustitución,
TTL de reservas, consolidación, export CSV).

## Overall Assessment

La mayoría de los flujos centrales **funcionan y son consistentes** entre backend y frontend
(ciclo de pedido, consolidación, OC/recepción, cobranzas, faltantes+notificaciones, CSV,
TTL config). Backend: **265 tests pasando, 0 fallos**. Frontend: build Next.js 16.3 OK.
Los dos bloqueantes P1 de C8 (BUG-002 autorización, BUG-003 sustitución con diferencia
negativa) fueron **resueltos y verificados en vivo**. Persisten como P1: BUG-004 (cierre de
jornada con EN_VIAJE colgados) y BUG-005 (decisión de alcance "pedido express"). Además
existe el riesgo estructural STR-001: el frontend completo está sin versionar de forma
reproducible (gitlink a commit vacío, sin submódulo, sin remote).

**Hardening de Producción cerrado y verificado (2026-08-16):**
- STR-004: orden determinista en TODA consulta (`express DESC, fechaCreacion DESC, id ASC`, null-safe).
- STR-005: `POST /api/test/reset` (dev/test) + `globalSetup` en Playwright para DB limpia/reproducible; NO existe en prod.
- STR-006: 4 DTOs migrados a `List<@Valid X>` → cero warnings HV000271.
- JWT_SECRET estricto en prod: arranque FALLA (`IllegalStateException`) si falta o usa el default inseguro.
- Métricas: `/actuator/metrics` + `/actuator/prometheus` (micrometer) protegidos por `ADMINISTRATIVO`.
- Docker: HEALTHCHECK en backend (→ `/api/actuator/health`) y frontend (→ `/login`); compose con healthchecks + `service_healthy`.

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
| C7 PWA repartidor /turno | sí | sí | parcial | parcial | manifest, SwipeButton, wizard /turno (3 fases); sin tests FE, sin drive visual |
| C8.1 Pedido express + alerta "en ruta hoy" | parcial | parcial | parcial | parcial | alerta presente; **"express" NO implementado** (BUG-005); alerta sin acotar a hoy (QA-08) |
| C8.2 Marcar faltante/dañado + notificaciones | sí | sí | sí | sí (backend) / parcial (visual) | 200, merma, notif no-leída, /no-leidas |
| C8.3 Sustitución en destino / cierre camión | sí | sí | sí | **sí** | BUG-003 resuelto+verificado (201 dif. negativa); resta QA-09 (UI en turno) |
| C8.4 Reserva TTL 48h auto-cancel | parcial | sí (unit) | parcial | parcial | ReservaTtlJob + config + tests; auto-cancel 48h no verificable en ventana |
| C8.5 Consolidación de pedidos hijos | sí | sí | sí | sí | 201, origenes RECHAZADO (QA-12 menor: pierde fechaJornada/escala) |
| C8.6 Export CSV | sí | sí | sí | sí | 5/5 → 200 text/csv; reportes 403 para repartidor |

## Completed

* Backend F0→F6 + B1→B5 + C1→C8 + D1→D4 implementado y compilando (265 tests verdes).
* Frontend Next.js 16.3 completo (login, dashboard, pedidos, stock, clientes/items/usuarios/
  proveedores, rutas+entregas, OC, cobranzas+caja, turno) — build OK.
* Migraciones Flyway V1→V22 validadas.
* Docker backend/frontend + compose local (Podman) funcionando.
* CI (GitHub Actions) definido: backend test + frontend build + E2E Playwright (8 tests).
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
- **Saneamiento Bloque P2.B (modelado de stock) aplicado y verificado:**
  - AUD-008: trazabilidad de proveedor en lote (`V18__lote_proveedor.sql`), recepción de OC asocia proveedor, `GET /stock/lotes?proveedorId=`.
  - AUD-009: estado de lote (`V19__lote_estado.sql` VIGENTE/AGOTADO/VENCIDO/DESCARTADO) + `POST /stock/lotes/{id}/descartar`; FEFO excluye descartados.
  - AUD-010: `disponibleDeLote` contempla `AJUSTE_INVENTARIO` por lote (ajuste con `loteId` opcional).
- **4 bloques de mejoras de compra/stock aplicados y verificados (2026-08-17):**
  - **Merma con signo negativo** (`V20__normalizar_merma_signo.sql` normaliza históricas) + fix asignar categoría a item en frontend + **ABMC de Zonas** (backend PUT/PATCH/GET por id + vista `/zonas` + nav; escrituras exigen ENCARGADO_DEPOSITO/ADMINISTRATIVO).
  - **Relación proveedor↔item** (`V21__proveedor_item.sql`, catálogo de provisión): `PUT/GET /proveedores/{id}/items`, validación de OC con `ITEM_NO_PROVISTO_POR_PROVEEDOR`, auto-vinculación en recepción.
  - **OC sin precio**: las líneas de OC llevan solo item+cantidad; el precio real se captura en la recepción (obligatorio) y en el ingreso manual (opcional), persistiendo en `lote.precio_unitario` (`V22__precio_oc_lote.sql`).
  - **Importación CSV de recepción**: `POST /stock/ingresos/csv` (sin OC, proveedorId opcional) y `POST /ordenes-compra/{id}/recepciones/csv` (vinculada a OC); formato `sku;cantidad;precioUnitario;fechaVencimiento;codigoLote`; errores por fila (400, transaccional). Frontend: botones de importación CSV.
- **Reestructuración de roles y UI aplicada y verificada (2026-08-17, Etapas 1 y 2):**
  - **Etapa 1 — Autorización por rol (backend):** rutas (creación/asignación de pedidos solo `ADMINISTRATIVO`; iniciar/cerrar/get `REPARTIDOR`+`ADMIN`), Zonas (ABMC solo `ADMINISTRATIVO`; `GET` authenticated), y Cobranzas (`VENDEDOR`+`ADMIN`+`REPARTIDOR`, con exigencia de `pedidoId` de una ruta propia NO finalizada para el repartidor — `COBRANZA_REPARTIDOR_SIN_PEDIDO` / `COBRANZA_PEDIDO_NO_EN_RUTA`). `DataSeeder`: `admin@pedidos.com` pasa a rol único `ADMINISTRATIVO`; se agregan `vendedor@pedidos.com` (VENDEDOR) y `deposito@pedidos.com` (ENCARGADO_DEPOSITO); `repartidor@pedidos.com` (REPARTIDOR). Matriz de seguridad cubierta por tests por rol.
  - **Etapa 2 — UI por rol (frontend):** barra lateral declarativa por rol (REPARTIDOR solo "Mi Jornada"; VENDEDOR Panel/Pedidos/Clientes/Catálogo/Cobranzas; ENCARGADO_DEPOSITO Panel/Pedidos/Stock/Items/Clientes/Proveedores/Compras; ADMIN todo agrupado). Categorías como pestaña en `/items`; Zonas como pestaña en `/clientes` (solo ADMIN). `/turno` = wizard del repartidor (Antes de salir → En viaje con paradas [entregar/cobrar/sustituir/reagendar/rechazar] → Rendición/Cerrar). Dashboard role-aware sin 403 para VENDEDOR/ENCARGADO.
  - **Fixes:** fechas `YYYY-MM-DD` como local (no UTC) en `format.ts`; `CobranzaRequest.clienteId` `@NotNull` (400 en vez de 500). **265 tests backend + build frontend verde; Flyway V22.**
- **Hardening de Producción aplicado y verificado (2026-08-16):**
  - STR-004: `listarPaginado` ordena toda consulta deterministamente (`express DESC, fechaCreacion DESC, id ASC`, null-safe); test `listarColaConfirmacionOrdenaDeterminista`.
  - STR-005: `POST /api/test/reset` (dev/test) con `TRUNCATE CASCADE` de 20 tablas + reseed; `globalSetup` en Playwright; no existe en prod (test `TestResetNoExisteEnProdTest`).
  - STR-006: 4 DTOs a `List<@Valid X>` (CrearPedidoRequest, EntregaRequest, RecepcionRequest, CrearOrdenCompraRequest) → cero HV000271.
  - JWT_SECRET fail-fast en prod (`IllegalStateException` si null/blank/default); `JwtServiceTest` (4 casos).
  - Métricas Prometheus: `micrometer-registry-prometheus` + `/actuator/metrics` y `/actuator/prometheus` (ADMINISTRATIVO).
  - Docker HEALTHCHECK (backend → `/api/actuator/health`, frontend → `/login`) + compose con healthchecks y `service_healthy`.

## Partially Completed

* C8.1 pedido "express" (solo alerta, sin flag) — BUG-005.
* C8.3 sustitución (solo diferencia positiva, solo ENTREGADO, sin UI turno) — BUG-003, QA-09.
* C8.4 TTL auto-cancel 48h (config+tests presentes; comportamiento temporal no verificado E2E).
* C7 PWA / turno — el **wizard del repartidor** (3 fases: Antes de salir → En viaje con paradas → Rendición/Cerrar) ya está implementado en `/turno`; quedan pendientes tests frontend y verificación visual.

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
* Sin monitoreo/actuator de producción (Fase D). — ✅ D3: actuator base (health/info) implementado; — ✅ Hardening: `/actuator/metrics` y `/actuator/prometheus` disponibles (protegidos por ADMINISTRATIVO).
* Sin tests e2e (Playwright) (Fase D). — ✅ D4: Playwright E2E implementado (7 tests: login, repartidor/turno, consolidación, sustitución, pedido express) + job CI; — ✅ Hardening: 8 tests (globalSetup con reset de DB).
* Sin tests del frontend (ningún test unit/e2e FE).
* JWT_SECRET con default inseguro en dev. — ✅ Hardening: fail-fast en prod (arranque falla si falta/usa default).
* Consultas de reportes sin optimizar para gran volumen (Fase D).

## Technical Debt

* ~~STR-004 (orden cola confirmación)~~ → **RESUELTO** (orden determinista global).
* ~~STR-005 (reset de datos E2E)~~ → **RESUELTO** (`POST /api/test/reset`, dev/test).
* ~~STR-006 (HV000271)~~ → **RESUELTO** (DTOs a `List<@Valid X>`, cero warnings).
* ~~JWT_SECRET default inseguro~~ → **RESUELTO** (fail-fast en prod).
* ~~Sin métricas/actuator de observabilidad~~ → **RESUELTO** (`/actuator/metrics`, `/actuator/prometheus`).
* ~~Sin healthchecks en Docker~~ → **RESUELTO** (HEALTHCHECK + compose `service_healthy`).
* `GlobalExceptionHandler` no mapea 404/405 → 500 (QA-01). — ✅ resuelto (404/405/415 mapeados).
* Validación anidada inconsistente entre requests (`@Valid` faltante en varios). — ✅ resuelto (STR-006).
* Frontend usa `size=500` sin paginar en turno y alertas (riesgo a gran escala) — QA-08/QA-10.
* ~5 warnings ESLint `react-hooks/set-state-in-effect` (patrón setState en efecto en algunos componentes del frontend) — pendiente de corregir.
* `pedidoPerteneceARepartidor` filtra en memoria (recupera los pedidos de la ruta y valida pertenencia en Java); a futuro una query dedicada (¿query por ruta + repartidor + estado no finalizado) evitaría el barrido en memoria.
* Duplicación de lógica de recepción CSV entre `POST /stock/ingresos/csv` y `POST /ordenes-compra/{id}/recepciones/csv` — **refactor opcional** (unificar el pipeline de parseo+persistencia).
* Frontend completo **sin versionar** de forma reproducible (STR-001) — P1, pendiente.
* Sin tests frontend.
* ROADMAP.md desactualizado (109→132 tests, V1→V11→V15, C8 no reflejado). — ✅ Hardening: ROADMAP.md actualizado a 204/8 y hitos cerrados.

## Architecture

Backend hexagonal (controller → port/in → service → port/out → JPA) en Java 21 + Spring Boot
4.1, Postgres 16 + Flyway (V1→V22), seguridad JWT + BCrypt. Módulos: usuario, cliente (zonas),
stock (items/lotes/ledger/FEFO/mermas/ajustes/reservas/CSV), pedido (circuito PENDIENTE_*),
ruta (despacho/asignación), compra (proveedores/OC/catálogo de provisión/CSV),
cobranza (remitos/cobranzas/cuenta),
sustitucion, notificacion, reporte, categoria. Frontend Next.js 16.3 + Tailwind v4 + React 19, app router,
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
- Backend: **265 tests, 0 fallos** (surefire) en 21 archivos.
- Frontend: **sin tests unitarios; E2E con Playwright: 8 tests verdes, estable como gate de CI** (`npm run test:e2e`); `globalSetup` resetea la DB (`POST /api/test/reset`) para corridas reproducibles.

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
* ~~STR-004~~ · ~~STR-005~~ · ~~STR-006~~ — resueltos (Hardening)

Future:
* QA-08, QA-11, QA-12 (P3) · STR-002 (docs) · STR-003 (limpieza datos QA)

## Checkpoint

Commit:
- **e04061f** `checkpoint: close milestone Fase C (C1-C8)` — pusheado a `origin/main`
- frontend integrado al repo padre como carpeta normal (53 archivos; gitlink eliminado)
- **Fase D (D1-D4):** commits `048222b`, `47e71a4`, `8ad9b93`, `638e447` + **`2b3ff41` `checkpoint: close milestone Fase D (D1-D4)`** — pusheado a `origin/main`
- **Hardening de Producción:** **`3ba84b3`** `feat(hardening)` + **`12b002b`** `fix(security): restringir TestResetDataCleaner a perfiles dev/test + reset determinista` — 204 tests backend + 8 E2E verdes
- working tree limpio (verificado en auditoría de cierre)

## Next Milestone

**Hardening de Producción COMPLETO** (STR-004/005/006, JWT_SECRET estricto, métricas Prometheus,
HEALTHCHECKs en Docker). Próximo: **Despliegue / Entrega a producción**. Saneamiento funcional y de
dominio COMPLETO. Pendiente de resolución para producción real: STR-001 (frontend sin versionar de
forma reproducible), limpieza de datos QA (STR-003), decidir alcance de BUG-005 (pedido express),
y optativo API versioning.

## Recommended Next Action

1. **Resolver STR-001 (P1):** versionar el frontend de forma reproducible (submódulo con remote o
   directorio integrado + commit del trabajo real) antes del despliegue.
2. Desplegar a un entorno de producción con `JWT_SECRET` real (el arranque en `prod` falla si falta)
   y validar healthchecks de Docker (`/api/actuator/health`, `/login`).
3. Confirmar limpieza de datos de QA en BD local (STR-003).
4. Decidir el alcance de BUG-005 (pedido express) y resolver los P1 restantes (BUG-004 cierre de jornada).

## Notes for Future Agents

* El repo padre ahora tiene 3 commits; el checkpoint `e04061f` cierra Fase C e integra el
  frontend como carpeta normal (antes era un gitlink a un commit vacío, sin submódulo ni remote).
* El trabajo de Fase A está en los 2 primeros commits; Fase B y C en el checkpoint.
* Los tests pasan contra Postgres local (`jdbc:postgresql://localhost:5432/pedidos`).
* Aplicar los criterios de aceptación de BACKLOG.md; no usar Engram como backlog.
