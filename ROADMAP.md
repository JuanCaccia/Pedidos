# Roadmap — Sistema de Pedidos, Stock y Ventas

> Priorización acordada (2026-08): **Fase A completa** ✅ · **Fase B completa** ✅ ·
> **Fase C completa** ✅ · **Fase D COMPLETA** ✅ (cimientos técnicos, resiliencia y QA) ·
> **Saneamiento Funcional (P1/P2) COMPLETO** ✅ · **Hardening de Producción COMPLETO** ✅.

## Estado actual

- **Backend** completo (F0→F6 + B1→B5): `usuario`, `cliente` (zonas), `stock`
  (items, lotes, ledger, FEFO por lote, mermas, ajustes con umbral, reservas),
  `pedido` (circuito con `PENDIENTE_STOCK`, pedido hijo), `ruta` (despacho +
  asignación), `compra` (proveedores + OC con recepción → stock), `cobranza`
  (remitos + cobranzas + cuenta de cliente), `reporte` (stock/ventas/rutas/caja),
  `sustitucion`, `notificacion`, seguridad JWT + BCrypt, auditoría, springdoc, actuator (health/info/metrics). **265 tests**.
- **Frontend** completo: login, Dashboard (alertas: stock bajo, pendientes,
  re-agendados, lotes por vencer), Pedidos (cliente+zona, acciones por rol),
  Stock (operativo), Clientes/Items/Usuarios/Proveedores (ABMC), Rutas +
  Entregas, Órdenes de Compra, Cobranzas + Caja. Next.js 16 + Tailwind v4,
  auditado con Impeccable.
- Migraciones Flyway V1→V22 · Dockerfile backend/frontend + compose · CI en
  GitHub Actions · App local en containers Podman (`:8080/api` + `:3000`).
- **Reestructuración de roles y UI COMPLETA** (Etapas 1 y 2): matriz de
  autorización por rol redefinida (rutas/Zonas/Cobranzas) + barra lateral por
  rol, tabs de Categorías/Zonas y wizard del repartidor en `/turno`. **265 tests backend.**
- Observabilidad base (D3): `/actuator/health` público (con liveness/readiness y detalles DB por rol), `/actuator/info` solo `ADMINISTRATIVO`. — ✅ Hardening: `/actuator/metrics` y `/actuator/prometheus` (micrometer) también solo `ADMINISTRATIVO`.

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

## Fase D — Cimientos técnicos, resiliencia y QA ✅ COMPLETA

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

### D4 — Tests E2E (Playwright) ✅ COMPLETA
- `@playwright/test` + chromium en el frontend; `playwright.config.ts` (baseURL :3000, reuseExistingServer).
- 8 tests E2E: login válido, login inválido, navegación repartidor, crear pedido express,
  flujo repartidor/turno (ENTREGADO), consolidación de pedidos, sustitución en destino (+cobranza).
- Script `npm run test:e2e` + job `e2e` en CI (postgres + backend + frontend + playwright).
- Suite estable como gate de CI (verificado 8/8 ×2).

---

## Saneamiento Funcional (P1/P2) ✅ COMPLETO

### P1 — Integridad de dominio y usabilidad crítica ✅ COMPLETA
- P1.A integridad de dominio: validación de reglas de negocio críticas
  (estados cobrables, reservas, FEFO, consolidación).
- P1.B usabilidad crítica: corrección de flujos operativos bloqueantes
  (autorización por rol, sustitución con diferencia negativa, cierre de jornada).

### P2 — Catálogo, accesos y modelado de stock ✅ COMPLETA
- P2.A catálogo, accesos y navegabilidad: categorías (corte limpio V17), búsqueda
  server-side, navegación por estados.
- P2.B modelado de stock: estado de lote (V18-V19), ajustes firmados, disponibleDeLote.

---

## Hardening de Producción ✅ COMPLETO

Deuda técnica no bloqueante resuelta + preparación para producción real (2026-08-16).

### H1 — Determinismo y reset E2E ✅ COMPLETA
- STR-004: `listarPaginado` ordena toda consulta (`express DESC, fechaCreacion DESC, id ASC`, null-safe).
- STR-005: `POST /api/test/reset` (dev/test, `TRUNCATE CASCADE` + reseed) + `globalSetup` en Playwright; no existe en prod.

### H2 — Validación, secretos y observabilidad ✅ COMPLETA
- STR-006: 4 DTOs a `List<@Valid X>` → cero HV000271.
- JWT_SECRET fail-fast en `prod` (arranque falla si falta/usar default).
- Métricas Prometheus: `/actuator/metrics` + `/actuator/prometheus` (ADMINISTRATIVO).

### H3 — Docker healthchecks ✅ COMPLETA
- HEALTHCHECK backend (→ `/api/actuator/health`) y frontend (→ `/login`); compose con healthchecks y `frontend.depends_on.backend.condition: service_healthy`.

Suite: **265 tests backend (21 archivos)** + **8 E2E (6 specs)** + build frontend verde.

---

## Reestructuración de roles y UI ✅ COMPLETO (2026-08-17)

### Etapa 1 — Autorización por rol (backend)
- **Rutas:** `POST /rutas` y `POST /rutas/{id}/pedidos` → solo `ADMINISTRATIVO`
  (planificación de Logística/Admin); `iniciar`/`cerrar`/`GET` → `REPARTIDOR`+`ADMINISTRATIVO`.
- **Zonas:** ABMC (POST/PUT/PATCH) → solo `ADMINISTRATIVO`; `GET` authenticated.
- **Cobranzas:** `POST /cobranzas` → `VENDEDOR`+`ADMINISTRATIVO`+`REPARTIDOR`. Si el actor
  es `REPARTIDOR` exige `pedidoId` de una ruta propia NO finalizada
  (`COBRANZA_REPARTIDOR_SIN_PEDIDO`, `COBRANZA_PEDIDO_NO_EN_RUTA`).
- **DataSeeder:** `admin@pedidos.com` pasa a rol único `ADMINISTRATIVO`; nuevos
  `vendedor@pedidos.com` (VENDEDOR) y `deposito@pedidos.com` (ENCARGADO_DEPOSITO).

### Etapa 2 — UI por rol (frontend)
- Barra lateral declarativa por rol (REPARTIDOR solo "Mi Jornada"; VENDEDOR
  Panel/Pedidos/Clientes/Catálogo/Cobranzas; ENCARGADO_DEPOSITO Panel/Pedidos/Stock/Items/
  Clientes/Proveedores/Compras; ADMIN todo agrupado).
- Categorías como pestaña en `/items`; Zonas como pestaña en `/clientes` (solo ADMIN).
- `/turno` = wizard del repartidor (Antes de salir → En viaje con paradas
  [entregar/cobrar/sustituir/reagendar/rechazar] → Rendición/Cerrar).
- Dashboard role-aware (sin 403 para VENDEDOR/ENCARGADO).

### Fixes
- Fechas `YYYY-MM-DD` como local (no UTC) en `format.ts`.
- `CobranzaRequest.clienteId` `@NotNull` → 400 en vez de 500.
