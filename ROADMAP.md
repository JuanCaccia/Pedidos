# Roadmap — Sistema de Pedidos, Stock y Ventas

> Priorización acordada (2026-08): **Fase A es la prioridad**. La Fase B se plantea
> según el resultado final de la A. Las Fases C y D quedan **postergadas** y entrarán
> en discusión recién al final de la hipotética Fase B.

## Estado actual

- **Backend** operativo (F0→F6): `usuario`, `cliente` (zonas), `stock` (items, lotes,
  ledger `MovimientoStock`, mermas, ajustes, reservas atómicas), `pedido` (circuito de
  8 estados, reserva parcial + backorder, pedido hijo por saldo), `ruta`, `reportes`,
  seguridad JWT + BCrypt, auditoría `created_by`, OpenAPI/springdoc. **58 tests**.
- **Frontend** (slice F1): login JWT, Dashboard, Pedidos (crear/confirmar/hijos),
  Stock (reporte + movimientos), Clientes, Usuarios (solo `ADMINISTRATIVO`),
  Items (ABMC completo). Next.js 16 + Tailwind v4.
- Migraciones Flyway V1→V7 · App local en `:8080/api` + `:3000` (proxy `/api`).

---

## Fase A — Cerrar el dominio (PRIORIDAD)

Objetivo: hacer el sistema **100 % operativo desde la UI**.

| # | Item | Esfuerzo | Notas |
|---|---|---|---|
| A1 | **Rutas + Entregas (frontend)**: crear ruta, asignar pedidos, iniciar jornada, registrar entregas total/parcial (→ pedido hijo), cerrar jornada | medio | El mayor hueco operativo: la entrega existe en backend pero no es usable desde la UI |
| A2 | **Stock operativo (frontend)**: Ingresos (con lote), Mermas, Ajustes — solo `ENCARGADO_DEPOSITO` | medio | El depósito no puede operar sin UI |
| A3 | **Cambio de contraseña**: `PUT /usuarios/{id}/password` + pantalla | chico | Hoy el único camino es desactivar/recrear |
| A4 | **Reactivar items**: `PATCH /items/{id}/reactivar` + botón | trivial | Baja reversible, igual que usuarios/clientes |
| A5 | **Alertas operativas**: pedidos con `pendiente_stock`, stock bajo, lotes por vencer (dashboard + filtros) | medio | Convierte backorder y vencimientos en acciones |
| A6 | **Refinamientos backend**: paginación en listados, `@Valid` declarativo, mensajes de error en español | medio | Deuda técnica acumulada |
| A7 | **Calidad**: tests de integración MockMvc, primeros tests del frontend, CI en GitHub Actions (`mvn test` + `npm build` por push) | medio | La calidad no puede depender de la sesión |
| A8 | **Operación**: Dockerfile del backend + compose completo, `JWT_SECRET` real, (opcional) deploy | medio | Listo para correr fuera de la máquina local |

**Criterio de salida de la Fase A:** un vendedor crea/confirma pedidos; el depósito
carga ingresos/mermas/ajustes; el administrativo planifica la ruta; el repartidor
entrega (parcial incluido) y todo se refleja en stock + reportes — todo desde la UI.

---

## Fase B — Modelo comercial (SE PLANTEA según el resultado final de la Fase A)

> No arrancar hasta que la Fase A esté cerrada y validada en operación real.
> El alcance definitivo de B se define con los aprendizajes de A.

Candidatos:

1. **Cobranzas / Remitos / Facturación** — el circuito de entrega dice
   *"se liquida la cobranza/remito por lo entregado"*, pero esa entidad no existe.
   Pieza que falta para cerrar venta → cobro.
2. **Proveedores + Órdenes de Compra** — hoy el ingreso es manual; modelar OC da
   trazabilidad real de reposición.
3. **Precios de lista** por item (y por zona/cliente) para sugerir en el pedido.
4. **FIFO/FEFO por lote en egresos** — registrar qué lote se consume
   (indispensable con vencimientos).
5. **Resumen de caja** por jornada / vendedor / ruta.

---

## Fase C y D — Postergadas (en discusión al final de la hipotética Fase B)

> No planificar ni estimar hasta entonces.

- **C — Experiencia y producto**: dashboard analítico con gráficos, app/PWA del
  repartidor (o mobile-first), notificaciones (email/WhatsApp), exportación
  CSV/PDF, multi-depósito / multi-empresa, integraciones externas (AFIP,
  pasarelas de pago).
- **D — Cimientos técnicos**: CI/CD completo, monitoreo (actuator/métricas),
  API versioning, tests e2e (Playwright), índices/consultas de reportes
  optimizadas.
