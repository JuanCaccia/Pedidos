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

### QA-04 — Validación anidada ausente en pedidos/entregas ✅ RESUELTO

- **Tipo:** bug · **Prioridad:** P2 · **Área:** validación
- **Repro:** `CrearPedidoRequest.items` sin `@Valid` (línea sin itemId → 500); `EntregaRequest` sin validación (cantidad negativa → 409 en vez de 400).
- **Esperado:** 400 con `fieldErrors`. **Actual:** ✅ Resuelto — `@NotEmpty @Valid` en `CrearPedidoRequest.items` y `EntregaRequest.entregas`; `EntregaLineaRequest.cantidadEntregada` a `@Positive`.
- **Evidencia:** verificado live (400 con `fieldErrors['items[0].itemId']`) + tests `pedidoConLineaSinItemIdDevuelve400`, `entregaConListaVaciaDevuelve400`, `entregaConCantidadCeroDevuelve400`.
- **Origen:** QA-04 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-05 — `POST /sustituciones` sin `@Valid` → 500 con body malformado ✅ RESUELTO

- **Tipo:** bug · **Prioridad:** P2 · **Área:** validación
- **Repro:** `POST /api/sustituciones {}` → 500.
- **Esperado:** 400. **Actual:** ✅ Resuelto — `SustituirRequest` con `@NotNull`/`@Positive` y `@Valid` en el controller → 400 `VALIDATION_ERROR`.
- **Evidencia:** verificado live (400) + test `sustitucionConBodyVacioDevuelve400`.
- **Origen:** QA-05 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-08 — Alerta "en ruta hoy" no acotada a la jornada + truncamiento size=500 ✅ RESUELTO

- **Tipo:** mejora · **Prioridad:** P3 · **Área:** frontend/alertas
- **Repro:** `pedidos/page.tsx:107` y `turno/page.tsx:172` marcan alerta si el cliente tiene cualquier pedido `EN_VIAJE`, sin filtrar por fecha; `size=500`.
- **Esperado:** alerta solo por pedidos EN_VIAJE de la jornada actual; sin truncamiento.
- **AC:** acotar por fecha y paginar correctamente.
- **Estado:** ✅ **Resuelto** — filtro backend `GET /pedidos?estado=EN_VIAJE&fechaJornada=YYYY-MM-DD` (usa `pedido.fechaJornada`); frontend pagina por páginas sin depender de `size=500`.
- **Origen:** QA-08 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-09 — Sustitución solo sobre ENTREGADO y sin UI en el turno ✅ RESUELTO

- **Tipo:** mejora · **Prioridad:** P2 · **Área:** sustituciones / turno
- **Repro:** `SustitucionService` solo permite `ENTREGADO`/`ENTREGADO_PARCIAL` (EN_VIAJE → 409); el turno no expone sustituciones.
- **Esperado:** sustitución en destino (EN_VIAJE/EN_CURSO) y ofrecida en la vista de turno.
- **AC:** permitir sustitución en estados de viaje y exponerla en turno.
- **Estado:** ✅ **Resuelto** — `SustitucionService` acepta `ENTREGADO`/`ENTREGADO_PARCIAL`/`EN_VIAJE`; en EN_VIAJE egresa el original (FEFO) antes de ingresar el sustituto (no infla stock, `CANTIDAD_EXCEDE_RESERVA`). UI en turno via componente compartido `SustitucionModal.tsx` (listado por `cantidadReservada` en EN_VIAJE, `cantidadEntregada` en ENTREGADO).
- **Origen:** QA-09 · **Milestone:** C8 / Fase D · **Estado:** resuelto

### QA-10 — Vista Turno carga todos los pedidos (`/api/pedidos?size=500`) ✅ RESUELTO

- **Tipo:** mejora (escalabilidad) · **Prioridad:** P2 · **Área:** turno
- **Repro:** `turno/page.tsx:62` pide `size=500` sin filtro; >500 pedidos → EN_VIAJE puede quedar fuera.
- **Esperado:** cola acotada a rutas del repartidor.
- **AC:** filtrar por ruta/repartidor/estado en backend.
- **Estado:** ✅ **Resuelto** — el turno resuelve sus pedidos con `GET /pedidos?ids=` desde `selectedRuta.pedidoIds` (los adicionales por `clienteId`); sin `size=500` global.
- **Origen:** QA-10 · **Milestone:** C8 / Fase D · **Estado:** resuelto

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

### QA-13 — La sustitución no verifica que el ítem original pertenezca/esté entregado ✅ RESUELTO

- **Tipo:** mejora / deuda técnica · **Prioridad:** P2 · **Área:** sustituciones
- **Repro:** `SustitucionService` no validaba que `itemOriginalId` sea un ítem del pedido; riesgo de movimientos espurios + cobranza arbitraria.
- **Esperado:** validar membresía del ítem original.
- **Actual:** ✅ Resuelto — `SustitucionService` valida `pedido.itemPorItem(itemOriginalId)` → `ITEM_NO_PERTENECE_AL_PEDIDO` antes de tocar stock.
- **Evidencia:** verificado live (409 `ITEM_NO_PERTENECE_AL_PEDIDO` sin registrar ingreso/ajuste) + test `sustituirItemAjenoAlPedidoLanza`.
- **Origen:** QA-13 · **Milestone:** C8 / Fase D · **Estado:** resuelto

---

## ESTRUCTURAL / REPRODUCTIBILIDAD

### STR-001 — Frontend no está versionado de forma reproducible (gitlink roto)

- **Tipo:** deuda técnica / riesgo de pérdida de trabajo · **Prioridad:** P1
- **Área:** Git / frontend
- **Estado:** ✅ **resuelto (verificado 2026-08-16)** — el frontend se integró como directorio normal del repo padre
- **Descripción:** `frontend/frontend` estaba trackeado en el repo padre como **gitlink (modo 160000)** apuntando a `af430cf` (commit vacío), **sin `.gitmodules`** → no era un submódulo declarado. El repo anidado no tenía remote y el trabajo real del frontend quedaba sin commitear.
- **Comportamiento esperado:** el frontend debe quedar versionado de forma reproducible (submódulo con remote o directorio integrado).
- **AC:** (a) decidir submódulo vs directorio integrado; (b) commitear el trabajo real; (c) `.gitmodules` correcto si aplica; (d) CI obtiene el frontend real.
- **Estado actual:** ✅ **Resuelto** — decisión tomada: **directorio integrado**. `frontend/frontend` tiene **63 archivos trackeados como blobs normales (100644)**, sin gitlink ni `.gitmodules`, y el repo tiene remote (`origin https://github.com/JuanCaccia/Pedidos.git`). La CI (`actions/checkout@v4` sin submodules) obtiene el frontend real. Verificado con `git ls-files -s frontend/frontend`.
- **Milestone:** C8 / cierre · **Estado:** resuelto

### STR-002 — ROADMAP.md desactualizado respecto al estado real

- **Tipo:** deuda de documentación · **Prioridad:** P2
- **Área:** docs
- **Descripción:** ROADMAP afirma "109 tests" (real: 132) y "Migraciones V1→V11" (real: V1→V15); no refleja C8 terminado ni los items bloqueantes.
- **AC:** actualizar conteos, migraciones y estado de C8.
- **Milestone:** C8 · **Estado:** abierto (se corregirá en PROJECT_STATUS, ROADMAP queda para próxima sesión)

### STR-003 — Higiene de datos de QA en BD viva ✅ RESUELTO

- **Tipo:** limpieza · **Prioridad:** P2
- **Área:** datos
- **Descripción:** la prueba QA creó items `QA-TEST` (id 5), `QA-TEST-2` (id 6) y clientes de prueba (incl. id 7 "Verif Auth SA", "Otro Cliente" qa) en la BD local `pedidos`. Requieren limpieza.
- **AC:** eliminar registros de prueba QA.
- **Estado:** ✅ **Resuelto** — el endpoint `POST /api/test/reset` (STR-005, perfil dev/test) hace `TRUNCATE ... RESTART IDENTITY CASCADE` de las 20 tablas transaccionales y reseedea el `DataSeeder`, dejando la BD dev/test en estado limpio y determinista. Ejecutar una vez el reset limpia cualquier dato residual de QA.
- **Milestone:** C8 / Hardening · **Estado:** resuelto

### STR-004 — Sin ordenamiento determinista en la cola de confirmación del backend ✅ RESUELTO

- **Tipo:** deuda técnica / mejora de backend · **Prioridad:** P2
- **Área:** pedidos / listado
- **Descripción:** `PedidoService.listarPaginado` ordena solo `PENDIENTE_STOCK`/`PENDIENTE_PREPARACION`/`PENDIENTE_ENTREGA` (express DESC, fecha ASC). `PENDIENTE_CONFIRMACION` se pagina en orden natural (id asc) → los pedidos nuevos quedan en la última página y la UI (tab "Conf.") no es determinista. Detectado al escribir el E2E de consolidación (que debió localizar paginando o usar fallback por API).
- **AC:** ordenar también la cola de confirmación (p. ej. más recientes primero) o definir un orden estable; revisar impactos en UI/QA.
- **Comportamiento actual:** ✅ Resuelto — `PedidoService.listarPaginado` ahora ordena TODA consulta por `estado` con `express DESC, fechaCreacion DESC, id ASC` (fechaCreacion null-safe).
- **Evidencia:** test `listarColaConfirmacionOrdenaDeterminista` agregado a `PedidoServiceTest`.
- **Origen:** QA E2E consolidación · **Milestone:** Hardening · **Estado:** resuelto (2026-08-16)

### STR-005 — Sin endpoints de limpieza/reset de datos para tests E2E ✅ RESUELTO

- **Tipo:** deuda técnica · **Prioridad:** P2
- **Área:** tests / backend
- **Descripción:** los specs E2E siembran pedidos/rutas/cobranzas por API y no hay endpoint de delete/reset. La BD crece entre corridas, lo que obliga a los tests a ser tolerantes a estado acumulado (localización paginada, fallback por API) y vuelve flaky cualquier test que asuma un conjunto fijo de datos.
- **AC:** (a) agregar endpoint/semilla de reset para entornos de test, o (b) correr E2E contra una BD reseteada por corrida; documentar la estrategia elegida.
- **Comportamiento actual:** ✅ Resuelto — nuevo `POST /api/test/reset` (controller `@Profile({"dev","test"})`, `TestResetController`): limpia 20 tablas con `TRUNCATE CASCADE` (`TestResetDataCleaner`) y reseedea vía `DataSeeder.seed()`. `SecurityConfig` lo deja `permitAll`. **NO existe en prod** (test `TestResetNoExisteEnProdTest`). Integrado en Playwright vía `frontend/frontend/e2e/global-setup.ts` (`globalSetup`).
- **Evidencia:** `TestResetNoExisteEnProdTest` (1 test) + E2E verde con DB limpia por corrida.
- **Origen:** QA E2E · **Milestone:** Hardening · **Estado:** resuelto (2026-08-16)

### STR-006 — Advertencia HV000271 (deprecación de `@Valid` sobre contenedores `List`) ✅ RESUELTO

- **Tipo:** deuda técnica menor · **Prioridad:** P3
- **Área:** validación
- **Descripción:** Hibernate Validator emite HV000271 al usar `@Valid` directamente sobre `List<...>` en `CrearPedidoRequest`/`EntregaRequest`. No afecta funcionalidad ni tests, pero conviene migrar a `@Valid` en el tipo parametrizado (p. ej. `List<@Valid LineaRequest>`) en un futuro.
- **Comportamiento actual:** ✅ Resuelto — 4 DTOs migrados a `List<@Valid X>`: `CrearPedidoRequest`, `EntregaRequest`, `RecepcionRequest`, `CrearOrdenCompraRequest`. Cero warnings HV000271.
- **Origen:** auditoría de cierre Fase D · **Milestone:** Hardening · **Estado:** resuelto (2026-08-16)

---

## AUDITORÍA FUNCIONAL — Dominio, Entidades y UX (2026-08-15)

> Hallazgos de la auditoría crítica delegada a sub-agentes de exploración. Todo verificado contra código (ruta:línea). Sin fixes aún.

### AUD-001 — Soft delete (`activo`) sin semántica real (P1) ✅ RESUELTO

- **Tipo:** bug de negocio · **Prioridad:** P1
- **Área:** Item / Cliente / Proveedor
- **Evidencia:** `PedidoService.crearPedido:88-90` valida solo `existeItem` (no `activo`); `ClienteGatewayImpl:19-21` no chequea `activo`; `OrdenCompraService:51-53` y `StockService:321-324` tampoco.
- **Problema:** items/clientes/proveedores desactivados se siguen usando en pedidos nuevos, OC, reservas y movimientos. Quedan en "limbo".
- **Impacto:** se venden/compran items dados de baja.
- **Estado:** ✅ **Resuelto (backend + frontend)** — backend rechaza item/cliente/proveedor inactivo en escritura (P1.A) y expone `?activos=true`; frontend envía `activos=true` en los comboboxes de creación (`pedidos/page.tsx:1021,1118,1187,1292`, `cobranzas/page.tsx:328`, `ordenes-compra/page.tsx:573`) resuelto en P2.A. Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-002 — FEFO no excluye lotes vencidos (P1) ✅ RESUELTO

- **Tipo:** bug de negocio · **Prioridad:** P1
- **Área:** stock / lotes
- **Evidencia:** `StockService.egresarPorLotes:196-226` ordena por `fechaVencimiento` pero no excluye `fechaVencimiento < hoy`; por ordenarse primero, entrega vencidos.
- **Impacto:** riesgo de despachar producto vencido.
- **Estado:** ✅ **Resuelto** — `StockService.egresarPorLotes` filtra lotes con `fechaVencimiento < LocalDate.now()` del pool de consumo (`StockService.java:202-203`). Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-003 — Cobranza sin validar pertenencia pedido↔cliente (P1 promovida) ✅ RESUELTO

- **Tipo:** bug de integridad · **Prioridad:** P1
- **Área:** cobranza
- **Evidencia:** `CobranzaService.registrar:35-48` no valida que `pedidoId` pertenezca a `clienteId` ni que el pedido exista/esté entregado.
- **Impacto:** estado de cuenta distorsionable; cobranza con pedido de otro cliente.
- **Estado:** ✅ **Resuelto** — `CobranzaService.registrar` valida existencia (NotFound), pertenencia al cliente (`:67`) y estado en `{EN_VIAJE, ENTREGADO, ENTREGADO_PARCIAL}` (`:29,:71`) con `COBRANZA_PEDIDO_INVALIDO`. Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-004 — Cambiar categoría en NuevoPedidoForm borra items agregados (P1) ✅ RESUELTO

- **Tipo:** bug de UX · **Prioridad:** P1
- **Área:** frontend pedidos
- **Evidencia:** `pedidos/page.tsx:1250-1253` setea `itemId:null` en todas las líneas al cambiar categoría; luego `:1143` descarta silenciosamente líneas sin item.
- **Impacto:** pedidos creados incompletos sin feedback; re-trabajo del vendedor.
- **Estado:** ✅ **Resuelto** — el `onChange` de categoría ya NO resetea `itemId` (solo `setCategoria`); cada línea conserva su `itemLabel`; `key={linea.key}` sin categoría; `Combobox.tsx` gana prop `valueLabel` para mostrar el label sin depender del search. Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-005 — Turno del repartidor no muestra domicilio ni paradas restantes (P1) ✅ RESUELTO

- **Tipo:** gap operativo · **Prioridad:** P1
- **Área:** frontend turno
- **Evidencia:** `turno/page.tsx` `PedidoCard:559-613` muestra numero/cliente/items/total pero NO domicilio (existe en tipo `Cliente`) ni observaciones ni lista de paradas restantes.
- **Impacto:** entregas fallidas/re-trabajo; sin planificación del recorrido.
- **Estado:** ✅ **Resuelto** — `PedidoCard` recibe `cliente` completo y `paradasRestantes`; muestra domicilio (📍 o "Sin domicilio cargado"), observaciones del pedido, y lista "Próximas paradas (N)". Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-006 — Stock no expone lotes vencidos/por vencer/descartados (P1) ✅ RESUELTO

- **Tipo:** gap operativo · **Prioridad:** P1
- **Área:** frontend stock / backend consultas
- **Evidencia:** `stock/page.tsx:172-239` tabla sin vencimiento; `/stock/lotes/por-vencer` existe pero `LoteResponse` no expone saldo; el dashboard "Lotes por vencer" linkea a `/stock` que no lo muestra.
- **Impacto:** encargado no detecta preventivamente qué vence; entrega de vencido / pérdida silenciosa.
- **Estado:** ✅ **Resuelto** — `LoteResponse` expone `disponible`, `estado` (VENCIDO/AGOTADO/VIGENTE), `itemNombre`, `itemSku`; `disponibleDeLote` sube al port; nuevo `GET /stock/lotes`; frontend `/stock` con sección de Lotes y filtros (Todos/Por vencer/Vencidos/Agotados). Verificado por QA.
- **Nota (limitación pendiente):** la tabla principal de `/stock` carga de `/api/reportes/stock` (solo ADMINISTRATIVO por SecurityConfig); un ENCARGADO_DEPOSITO recibe 403 en esa tabla. La sección de Lotes sí es accesible. Pendiente: abrir el stock por item al rol depósito.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-007 — Categoría de Item como texto libre (P2) ✅ RESUELTO

- **Tipo:** deuda de modelado · **Prioridad:** P2
- **Área:** Item / categoría
- **Evidencia:** `V12__item_categoria.sql:1` columna string; sin tabla `categoria`; `listarCategorias` = `DISTINCT` derivado.
- **Impacto:** sin normalización; typos crean categorías; imposible renombrar/estandarizar.
- **Estado:** ✅ **Resuelto (corte limpio)** — migración `V17__categoria.sql` (tabla `categoria` + backfill + `item.categoria_id` FK + **DROP de `item.categoria`** sin legado). Módulo hexagonal `categoria` (CRUD + activo), `Item` pasa a `categoriaId`/`categoriaNombre`, `listarCategorias` lee la tabla. Frontend: selector por `categoriaId` en item y pedidos (preservando P1.B), gestión de categorías en `/items`. Verificado por QA: 181 backend + 8 E2E + build verde.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-008 — Proveedor sin trazabilidad con Item/Lote (P2) ✅ RESUELTO

- **Tipo:** deuda de modelado · **Prioridad:** P2
- **Área:** Proveedor / compra / stock
- **Evidencia:** `ProveedorJpaEntity:9-25` sin relación JPA con item/lote; `registrarIngreso` no pasa proveedor; `lote` no tiene `proveedor_id`.
- **Impacto:** no se responde "items por proveedor" ni "origen de stock".
- **Estado:** ✅ **Resuelto** — migración `V18__lote_proveedor.sql` (`lote.proveedor_id` FK nullable); la recepción de OC propaga el proveedor al ingreso/lote; `GET /stock/lotes?proveedorId=` consulta lotes por proveedor; `LoteResponse` expone `proveedorId`. `IngresoRequest` acepta `proveedorId`. Verificado por QA (198 backend + 8 E2E + build).
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-009 — Lote sin estado de ciclo de vida (P2) ✅ RESUELTO

- **Tipo:** deuda de modelado · **Prioridad:** P2
- **Área:** stock / lotes
- **Evidencia:** `lote` sin columna `estado`; `TipoMovimiento` sin DESCARTE/VENCIDO; merma no altera estado del lote.
- **Impacto:** no se puede marcar/cerrar un lote; agotados/vencidos siguen listados.
- **Estado:** ✅ **Resuelto** — migración `V19__lote_estado.sql` (`lote.estado` enum VIGENTE/AGOTADO/VENCIDO/DESCARTADO); ingreso → VIGENTE, egreso que agota → AGOTADO, VENCIDO derivado por fecha (prioridad DESCARTADO > VENCIDO > AGOTADO > VIGENTE); `POST /stock/lotes/{id}/descartar` (registra merma por saldo + setea DESCARTADO); `egresarPorLotes` excluye DESCARTADOS. Frontend: badge/filtro "Descartados" + botón Descartar. Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-010 — Ajuste de inventario no impacta disponible de lote (P2) ✅ RESUELTO

- **Tipo:** deuda de integridad · **Prioridad:** P2
- **Área:** stock
- **Evidencia:** `ajustarInventario` registra con `lote_id=null` y `disponibleDeLote` no contempla AJUSTE → incoherencia item vs lote; FEFO usa disponible de lote.
- **Impacto:** descuentos declarados que el FEFO ignora.
- **Estado:** ✅ **Resuelto** — `disponibleDeLote` ahora contempla `AJUSTE_INVENTARIO` por lote; `AjusteInventarioCommand`/`AjusteRequest` ganan `loteId` opcional (si viene valida pertenencia `AJUSTE_LOTE_INCOMPATIBLE`; si null es ajuste global). Opción A (más coherente). Verificado por QA.
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-011 — Dashboard con links genéricos/mismatch (P2) ✅ RESUELTO

- **Tipo:** mejora · **Prioridad:** P2
- **Área:** frontend dashboard
- **Evidencia:** `page.tsx:180,216,238,272` — "Stock bajo"→/items, "Lotes por vencer"→/stock (ciego), re-agendados/sin-stock sin pre-seleccionar tab.
- **Impacto:** alertas que no conducen a resolver el problema.
- **Estado:** ✅ **Resuelto** — links accionables con pre-filtrado: `/stock?filtro=bajo`, `/stock?tab=lotes&filtro=vencer`, `/pedidos?tab=PENDIENTE_STOCK`, `/pedidos?tab=RE_AGENDADO`; `stock/page.tsx` y `pedidos/page.tsx` leen `useSearchParams`; componente `Toast` global usado en sustitución y stock. Verificado por QA (8 E2E incluye smoke `dashboard.spec.ts`).
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

### AUD-012 — Acceso al stock por ítem para ENCARGADO_DEPOSITO + comboboxes solo activos ✅ RESUELTO

- **Tipo:** mejora de permisos · **Prioridad:** P2
- **Área:** permisos / frontend comboboxes
- **Descripción:** el rol `ENCARGADO_DEPOSITO` recibía 403 en `/stock` (la tabla carga de `/api/reportes/stock`, restringido a ADMINISTRATIVO). Además los comboboxes de creación no filtraban inactivos (pendiente del P1.A).
- **Estado:** ✅ **Resuelto** — `SecurityConfig` abre `/reportes/stock` y `/reportes/stock/exportar.csv` a `ENCARGADO_DEPOSITO`/`ADMINISTRATIVO` (el resto de reportes sigue solo ADMIN); comboboxes de creación (pedidos, cobranzas, ordenes-compra) envían `activos=true`. Test de integración: encargado ve stock (200), repartidor no ve reportes (403).
- **Origen:** auditoría funcional · **Milestone:** Saneamiento · **Estado:** resuelto

---

## MEJORAS RECIENTES (2026-08-17)

> Features/completados de los últimos 4 bloques de mejoras, todos **verificados**
> (254 tests backend, build frontend verde, Flyway V1→V22). No son bugs pendientes.

- **Merma con signo negativo** — las mermas se persisten con signo negativo
  (coherente con EGRESO_VENTA); migración `V20__normalizar_merma_signo.sql`
  normaliza las históricas positivas.
- **Fix asignar categoría a item en frontend** — al asignar/editar categoría de un
  item se persiste correctamente desde la UI.
- **ABMC de Zonas completo** — backend `PUT/PATCH/GET` por id + vista `/zonas` +
  nav; escrituras exigen `ENCARGADO_DEPOSITO`/`ADMINISTRATIVO`.
- **Relación proveedor↔item (catálogo de provisión)** — migración
  `V21__proveedor_item.sql`; `PUT/GET /proveedores/{id}/items`; validación de OC
  con `ITEM_NO_PROVISTO_POR_PROVEEDOR`; auto-vinculación en recepción.
- **OC sin precio** — las líneas de OC ya solo llevan item+cantidad; el precio real
  se captura en la recepción de OC (obligatorio) y en el ingreso manual (opcional),
  persistiendo en `lote.precio_unitario` (migración `V22__precio_oc_lote.sql`).
- **Importación CSV de recepción** — `POST /stock/ingresos/csv` (sin OC) y
  `POST /ordenes-compra/{id}/recepciones/csv` (vinculada a OC); formato
  `sku;cantidad;precioUnitario;fechaVencimiento;codigoLote`; errores por fila (400,
  transaccional). Frontend: botones de importación CSV.

---

## LIMITACIONES CONOCIDAS (no bugs)

- Sin soporte offline.
- Sin monitoreo de producción (actuator/metrics) — ✅ Hardening: `/actuator/metrics` y `/actuator/prometheus` expuestos (protegidos por `ADMINISTRATIVO`).
- Sin tests e2e (Playwright) — ✅ Hardening: suite E2E Playwright implementada (8 tests, 6 specs) con `globalSetup` que resetea la DB vía `POST /api/test/reset`.
- Sin tests del frontend.
- JWT_SECRET con default inseguro `cambiar-en-produccion...` (debe setearse en producción). — ✅ Hardening: en perfil `prod` el arranque FALLA con `IllegalStateException` si el secret es null/blank o igual al default inseguro.
