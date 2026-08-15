# Backlog — Sistema de Pedidos, Stock y Ventas

> Fuente de verdad de issues pendientes. **No** usar Engram como sustituto.
> Cada item registra: tipo, prioridad, área, estado, descripción, pasos de reproducción (bugs),
> comportamiento esperado/actual, criterios de aceptación, origen, milestone.

---

## CRITICAL (P1 — no cerran Fase C)

### BUG-002 — REPARTIDOR puede crear ítems y clientes (violación de autorización) ✅ RESUELTO

- **Tipo:** bug (seguridad)
- **Prioridad:** P1
- **Área:** Autorización / SecurityConfig
- **Estado:** resuelto (2026-08-15) — verificado live
- **Origen:** QA exploratorio (QA-02) — confirmado manualmente en este cierre
- **Descripción:** `POST /api/items` y `POST /api/clientes` no estaban restringidos por rol. Un token `REPARTIDOR` recibía **201** al crear un cliente.
- **Comportamiento esperado:** `POST /items` y `POST /clientes` requieren rol `VENDEDOR`/`ENCARGADO_DEPOSITO`/`ADMINISTRATIVO`; `REPARTIDOR` → 403.
- **Comportamiento actual:** ✅ Resuelto. `SecurityConfig` ahora exige `ENCARGADO_DEPOSITO`/`ADMINISTRATIVO` para `POST/PUT` de `/items`, y `VENDEDOR`/`ADMINISTRATIVO` para `POST/PUT` de `/clientes`. Frontend (`items`, `clientes`) oculta acciones de gestión a roles sin permiso.
- **Evidencia de verificación:** `POST /api/items` y `POST /api/clientes` con token repartidor → **403**; admin → **201** (verificado en vivo). Test `repartidorNoPuedeCrearItemsNiClientes` y `encargadoDepositoPuedeCrearItems` agregados a `PedidosIntegrationTest`.
- **Criterios de aceptación:** ✅ cumplidos (403 repartidor, 201 roles habilitados, tests de regresión).
- **Milestone:** C8

### BUG-003 — Sustitución con diferencia de precio negativa falla y revierte la transacción ✅ RESUELTO

- **Tipo:** bug
- **Prioridad:** P1
- **Área:** sustituciones / cobranzas
- **Estado:** resuelto (2026-08-15) — verificado live
- **Origen:** QA exploratorio (QA-03) — confirmado por código en este cierre
- **Descripción:** `SustitucionService.sustituir` calculaba `diferenciaPrecio` y la pasaba a `RegistrarCobranza`, que rechazaba montos `<= 0`. Con sustituto más caro que el original (diferencia negativa) lanzaba `VALIDATION_ERROR` y, al ser `@Transactional`, revertía todo.
- **Comportamiento esperado:** la sustitución se registra y la cobranza compensatoria usa el signo correcto.
- **Comportamiento actual:** ✅ Resuelto. `CobranzaService.registrar` ahora acepta montos con signo (rechaza solo nulo/cero); el endpoint HTTP `POST /cobranzas` mantiene validación `@Valid @DecimalMin(0.01)` para montos positivos. La compensación por sustitución persiste con el signo correcto.
- **Evidencia de verificación:** `POST /api/sustituciones` (original HAR 3.5 → sustituto ACE 6.0, qty 1, diferencia -2.50) → **HTTP 201**, `diferenciaPrecio: -2.50`, sin rollback (verificado en vivo). Test `montoCompensatorioNegativoPersiste` agregado a `CobranzaServiceTest`.
- **Criterios de aceptación:** ✅ cumplidos.
- **Milestone:** C8

---

## IMPORTANT (P1 — a revisar, no bloquean el cierre formal pendiente de triage)

### BUG-004 — Se puede cerrar la jornada con pedidos aún EN_VIAJE ✅ RESUELTO

- **Tipo:** bug
- **Prioridad:** P1 (integridad de rutas)
- **Área:** rutas
- **Estado:** resuelto (2026-08-15) — verificado live
- **Origen:** QA exploratorio (QA-06)
- **Descripción:** `RutaService.cerrarJornada` no validaba que no queden pedidos `EN_VIAJE`. Se cerraba la ruta `FINALIZADA` dejando pedidos `EN_VIAJE` "colgados".
- **Comportamiento esperado:** cerrar solo si todos los pedidos fueron entregados.
- **Comportamiento actual:** ✅ Resuelto. `PedidoGateway.estaEnViaje` + validación en `cerrarJornada`: lanza `PEDIDOS_EN_VIAJE` listando los números cuando hay pedidos en viaje.
- **Evidencia:** verificado live — cerrar con pedido EN_VIAJE → 409 `PEDIDOS_EN_VIAJE` "los pedidos PED-181060 siguen en viaje"; entregado → cierre 200. Tests `cerrarJornadaConPedidoEnViajeLanzaBusinessException` y `cerrarJornadaConTodosEntregadosCierra`.
- **Criterios de aceptación:** ✅ cumplidos.
- **Milestone:** C8 / Fase D

### BUG-005 — "Pedido express" (Opción A implementada) ✅ RESUELTO

- **Tipo:** mejora / decisión de alcance
- **Prioridad:** P1
- **Área:** pedidos
- **Estado:** resuelto (2026-08-15) — Opción A
- **Origen:** QA exploratorio (QA-07)
- **Descripción:** el escenario C8.1 refería a "pedido express" pero no existía flag.
- **Comportamiento actual:** ✅ Resuelto. Se eligió la **Opción A**: flag `express` (default false) en `Pedido` + migración `V16__pedido_express.sql` + ordenamiento prioritario de la cola de preparación/despacho (`express DESC, fecha_creacion ASC`) + badge "Express" en la UI y checkbox al crear pedido.
- **Evidencia:** verificado live — crear pedido con `express:true` devuelve `express:true`; test `crearPedidoExpressPersisteFlag` y `listarColaPreparacionOrdenaExpressPrimeroYLuegoPorFecha`.
- **Criterios de aceptación:** ✅ cumplidos.
- **Milestone:** C8 / Fase D

---

## MEJORAS / TÉCNICA (P2 / P3)

### QA-01 — Rutas inexistentes y métodos HTTP incorrectos devuelven 500 en vez de 404/405 ✅ RESUELTO

- **Tipo:** bug · **Prioridad:** P2 · **Área:** error handling
- **Repro:** `GET /api/nonexistent` → 500; `GET /api/stock/mermas` (POST-only) → 500.
- **Esperado:** 404 (recurso) / 405 (método). **Actual:** ✅ Resuelto — `GlobalExceptionHandler` mapea `NoResourceFoundException`→404, `HttpRequestMethodNotSupportedException`→405, `HttpMediaTypeNotSupportedException`→415.
- **Evidencia:** verificado live (404 / 405) + tests `recursoInexistenteDevuelve404`, `metodoNoSoportadoDevuelve405`.
- **Origen:** QA-01 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-04 — Validación anidada ausente en pedidos/entregas

- **Tipo:** bug · **Prioridad:** P2 · **Área:** validación
- **Repro:** `CrearPedidoRequest.items` sin `@Valid` (línea sin itemId → 500); `EntregaRequest` sin validación (cantidad negativa → 409 en vez de 400).
- **Esperado:** 400 con `fieldErrors`. **Actual:** 500/409 inconsistente.
- **AC:** `@Valid` en listas anidadas; `EntregaRequest` validado.
- **Origen:** QA-04 · **Milestone:** C8 · **Estado:** backlog

### QA-05 — `POST /sustituciones` sin `@Valid` → 500 con body malformado ✅ RESUELTO

- **Tipo:** bug · **Prioridad:** P2 · **Área:** validación
- **Repro:** `POST /api/sustituciones {}` → 500.
- **Esperado:** 400. **Actual:** ✅ Resuelto — `SustituirRequest` con `@NotNull`/`@Positive` y `@Valid` en el controller → 400 `VALIDATION_ERROR`.
- **Evidencia:** verificado live (400) + test `sustitucionConBodyVacioDevuelve400`.
- **Origen:** QA-05 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-08 — Alerta "en ruta hoy" no acotada a la jornada + truncamiento size=500

- **Tipo:** mejora · **Prioridad:** P3 · **Área:** frontend/alertas
- **Repro:** `pedidos/page.tsx:107` y `turno/page.tsx:172` marcan alerta si el cliente tiene cualquier pedido `EN_VIAJE`, sin filtrar por fecha; `size=500`.
- **Esperado:** alerta solo por pedidos EN_VIAJE de la jornada actual; sin truncamiento.
- **AC:** acotar por fecha y paginar correctamente.
- **Origen:** QA-08 · **Milestone:** C8 · **Estado:** backlog

### QA-09 — Sustitución solo sobre ENTREGADO y sin UI en el turno

- **Tipo:** mejora · **Prioridad:** P2 · **Área:** sustituciones / turno
- **Repro:** `SustitucionService` solo permite `ENTREGADO`/`ENTREGADO_PARCIAL` (EN_VIAJE → 409); el turno no expone sustituciones.
- **Esperado:** sustitución en destino (EN_VIAJE/EN_CURSO) y ofrecida en la vista de turno.
- **AC:** permitir sustitución en estados de viaje y exponerla en turno.
- **Origen:** QA-09 · **Milestone:** C8 · **Estado:** backlog

### QA-10 — Vista Turno carga todos los pedidos (`/api/pedidos?size=500`)

- **Tipo:** mejora (escalabilidad) · **Prioridad:** P2 · **Área:** turno
- **Repro:** `turno/page.tsx:62` pide `size=500` sin filtro; >500 pedidos → EN_VIAJE puede quedar fuera.
- **Esperado:** cola acotada a rutas del repartidor.
- **AC:** filtrar por ruta/repartidor/estado en backend.
- **Origen:** QA-10 · **Milestone:** C8 · **Estado:** backlog

### QA-11 — Login inválido devuelve 409 en vez de 401

- **Tipo:** mejora · **Prioridad:** P3 · **Área:** autenticación
- **Repro:** `POST /api/auth/login` credenciales erróneas → 409 (verificado en este cierre).
- **Esperado:** 401. **Actual:** 409 (semánticamente incorrecto).
- **AC:** 401 para credenciales inválidas.
- **Origen:** QA-11 · **Milestone:** C8 · **Estado:** backlog

### QA-12 — Pedido consolidado pierde `fechaJornada` y cambia la escala del total

- **Tipo:** mejora · **Prioridad:** P3 · **Área:** consolidación
- **Repro:** consolidar [69,70] → nuevo pedido `fechaJornada: null`, `total: 73.00000` vs origenes `73.00`.
- **Esperado:** heredar fechaJornada; escala consistente.
- **AC:** propagar fechaJornada y normalizar escala monetaria.
- **Origen:** QA-12 · **Milestone:** C8 · **Estado:** backlog

### QA-13 — La sustitución no verifica que el ítem original pertenezca/esté entregado

- **Tipo:** mejora / deuda técnica · **Prioridad:** P2 · **Área:** sustituciones
- **Repro:** `SustitucionService` no valida que `itemOriginalId` sea un ítem del pedido entregado; riesgo de movimientos espurios + cobranza arbitraria.
- **Esperado:** validar membresía del ítem original.
- **AC:** rechazar sustituciones de ítems no entregados en el pedido.
- **Origen:** QA-13 · **Milestone:** C8 · **Estado:** backlog

---

## ESTRUCTURAL / REPRODUCTIBILIDAD

### STR-001 — Frontend no está versionado de forma reproducible (gitlink roto)

- **Tipo:** deuda técnica / riesgo de pérdida de trabajo · **Prioridad:** P1
- **Área:** Git / frontend
- **Estado:** abierto (requiere decisión antes del checkpoint)
- **Descripción:** `frontend/frontend` está trackeado en el repo padre como **gitlink (modo 160000)** apuntando a `af430cf` (commit vacío "Initial commit from Create Next App"), pero **no existe `.gitmodules`** → no es un submódulo declarado. El repo anidado **no tiene remote** y **los 11 archivos reales del frontend (todo el app) están sin commitear** en su working tree. Un `actions/checkout` (CI) sin submodules obtendría un frontend vacío/incorrecto; el checkpoint del repo padre NO captura el trabajo real del frontend.
- **Comportamiento esperado:** el frontend debe quedar versionado de forma reproducible (declarar submódulo con remote + commit del trabajo, o integrarlo como directorio normal del repo padre).
- **AC:** (a) decidir submódulo vs directorio integrado; (b) commitear el trabajo real del frontend; (c) `.gitmodules` correcto si aplica; (d) CI obtiene el frontend real.
- **Bloquea milestone:** Sí, para entrega/reproductibilidad; es el punto más crítico del checkpoint.
- **Milestone:** C8 / cierre

### STR-002 — ROADMAP.md desactualizado respecto al estado real

- **Tipo:** deuda de documentación · **Prioridad:** P2
- **Área:** docs
- **Descripción:** ROADMAP afirma "109 tests" (real: 132) y "Migraciones V1→V11" (real: V1→V15); no refleja C8 terminado ni los items bloqueantes.
- **AC:** actualizar conteos, migraciones y estado de C8.
- **Milestone:** C8 · **Estado:** abierto (se corregirá en PROJECT_STATUS, ROADMAP queda para próxima sesión)

### STR-003 — Higiene de datos de QA en BD viva

- **Tipo:** limpieza · **Prioridad:** P2
- **Área:** datos
- **Descripción:** la prueba QA creó items `QA-TEST` (id 5), `QA-TEST-2` (id 6) y clientes de prueba (incl. id 7 "Verif Auth SA", "Otro Cliente" qa) en la BD local `pedidos`. Requieren limpieza.
- **AC:** eliminar registros de prueba QA.
- **Milestone:** C8 · **Estado:** abierto

---

## LIMITACIONES CONOCIDAS (no bugs)

- Sin soporte offline.
- Sin monitoreo de producción (actuator/metrics) — Fase D.
- Sin tests e2e (Playwright) — Fase D.
- Sin tests del frontend.
- JWT_SECRET con default inseguro `cambiar-en-produccion...` (debe setearse en producción).
