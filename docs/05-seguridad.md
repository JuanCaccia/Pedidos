# Security — JWT Authentication & Authorization

> Authentication is **stateless JWT**; the API has no server-side session.
> Passwords are hashed with **BCrypt**. CSRF is disabled (stateless API).

See [03-api.md](./03-api.md) for the full endpoint list. This document explains
how auth works and the exact role → route matrix.

---

## Authentication flow

1. `POST /api/auth/login` with `email` + `password` (this is the only public
   write endpoint).
2. `AuthService.login`:
   - Looks up the user by email → `AUTH_INVALIDO` if not found.
   - Checks `activo` → `AUTH_INACTIVO` if deactivated.
   - Verifies the BCrypt hash → `AUTH_INVALIDO` on mismatch.
   - Builds a JWT.
3. The token is signed with **HMAC-SHA** using `app.jwt.secret`, has a subject
   equal to `usuarioId`, carries claims `email` and `roles`, and expires after
   `app.jwt.expiration` ms (default **8 hours**).
4. Client sends `Authorization: Bearer <token>` on subsequent requests.
5. A **JWT filter** (registered before `UsernamePasswordAuthenticationFilter`)
   validates the token on every request and loads `usuario_roles` to build the
   security context.

### Token shape

```
Header:   { "alg": "HS256", "typ": "JWT" }
Payload:  { "sub": "<usuarioId>", "email": "...", "roles": [...], "exp": <epoch-ms> }
```

### Configuration highlights

| Setting | Value |
|---|---|
| Session | `STATELESS` |
| CSRF | off |
| Password encoder | BCrypt |
| JWT algorithm | HMAC-SHA |
| JWT expiration | 8 h (default) |
| JWT_SECRET (prod) | mandatory; app fails to start if missing/default (fail-fast) |

---

## Authorization matrix

The `SecurityConfig` defines role requirements. `AUTH` = any authenticated
user. Paths not listed fall back to **authenticated**.

| Path | Roles |
|---|---|
| `/auth/login` | Public |
| `/health`, `/actuator/health`, `/actuator/health/**` | Public |
| `/v3/api-docs/**`, `/swagger-ui/**` | Public |
| `/usuarios/**` | `ADMINISTRATIVO` |
| `POST /pedidos`, `POST /pedidos/{id}/confirmar` | `VENDEDOR`, `ADMIN` |
| `/pedidos/{id}/despachar`, `/agregar-stock`, `/marcar-faltante` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/pedidos/{id}/entregas`, `/reagendar` | `REPARTIDOR`, `ADMIN` |
| `/notificaciones/**` | `ADMIN`, `ENCARGADO_DEPOSITO` |
| `POST /stock/*` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/stock/ingresos/csv`, `/ordenes-compra/{id}/recepciones/csv` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `POST /rutas`, `POST /rutas/{id}/pedidos` | `ADMIN` (planificación de rutas = Logística/Admin) |
| `POST /rutas/{id}/iniciar`, `POST /rutas/{id}/cerrar`, `GET /rutas/**` | `REPARTIDOR`, `ADMIN` |
| `/reportes/stock` (+ CSV) | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/reportes/**` (rest) | `ADMIN` |
| `POST/PUT /items` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/categorias`: GET | authenticated |
| `/categorias`: POST/PUT/PATCH | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/clientes`: POST/PUT/PATCH | `VENDEDOR`, `ADMIN` |
| `POST /cobranzas` | `VENDEDOR`, `ADMIN`, `REPARTIDOR` (ver nota) |
| `/proveedores`, `/ordenes-compra/**` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/proveedores/{id}/items` | `ENCARGADO_DEPOSITO`, `ADMIN` |
| `/zonas`: GET | authenticated |
| `/zonas`: POST/PUT/PATCH | `ADMIN` |
| `POST /sustituciones` | `REPARTIDOR`, `ADMIN` |
| `/actuator/**` | `ADMIN` |
| `POST /test/reset` | Public (`permitAll`) — **active ONLY in `dev`/`test`; NOT registered in `prod`** |
| Everything else | authenticated |

### Cobranzas del repartidor

`POST /cobranzas` habilita a `REPARTIDOR`, pero acotado a su ruta: si el actor
es `REPARTIDOR` el request **exige** `pedidoId`, y ese pedido debe pertenecer a
una ruta del repartidor que aún NO esté finalizada. De lo contrario devuelve:

- `COBRANZA_REPARTIDOR_SIN_PEDIDO` — el repartidor envió cobranza sin `pedidoId`.
- `COBRANZA_PEDIDO_NO_EN_RUTA` — el `pedidoId` no pertenece a una ruta activa del repartidor.

El `VENDEDOR` y el `ADMIN` siguen pudiendo cobrar directo (con `clienteId`; `pedidoId` opcional).

### Usuarios seed

`DataSeeder` provee un usuario por rol, todos de rol único (a diferencia del
modelo anterior en que `admin` concentraba los cuatro):

| Email | Rol |
|---|---|
| `admin@pedidos.com` | `ADMINISTRATIVO` (solo) |
| `vendedor@pedidos.com` | `VENDEDOR` |
| `deposito@pedidos.com` | `ENCARGADO_DEPOSITO` |
| `repartidor@pedidos.com` | `REPARTIDOR` |

> Note: the app supports multiple roles per user, but the seed data deliberately
> gives each demo account a single role so role-based behavior is exercised in
> isolation.

---

## Failure responses

| Condition | Status |
|---|---|
| Not authenticated / bad token | `401 UNAUTHORIZED` |
| Authenticated but role insufficient | `403 FORBIDDEN` |

Both return a JSON error body (see [03-api.md](./03-api.md)).

---

## Actuator restrictions

- `/actuator/health` (with liveness/readiness probes) is **public**; health
  detail is shown only when authorized (`show-details: when-authorized`).
- `/actuator/info` (app metadata), `/actuator/metrics` and
  `/actuator/prometheus` (observability), and the rest of `/actuator/**` require
  `ADMINISTRATIVO`.

---

## Production configuration notes

- **`JWT_SECRET` is mandatory in production.** In the `prod` profile the
  application **fails to start** (`IllegalStateException`) if `app.jwt.secret` is
  null/blank **or** equals the insecure default (`cambiar-en-produccion...`).
  Set a strong secret (≥ 256 bits for HS256) via a real secret manager.

---

Continue to [tests](./06-tests.md) or [conventions](./07-convenciones.md).
