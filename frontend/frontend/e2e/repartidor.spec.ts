import { expect, test } from "@playwright/test";
import { login, REPARTIDOR } from "./helpers";

// ---------------------------------------------------------------------------
// E2E: FLUJO REPARTIDOR / TURNO
//
// Armado por API contra el backend (:8080, context-path /api). Todos los IDs
// se resuelven en runtime (nada hardcodeado). La vista /turno auto-selecciona
// la ruta EN_CURSO de fecha máxima, así que creamos nuestra ruta con una
// fechaJornada calculada (máx fecha EN_CURSO existente + 1 día) para que la
// auto-selección caiga SIEMPRE en nuestra ruta (determinismo: hay rutas
// EN_CURSO de seed con fechas hoy/mañana que ya están "entregadas").
// ---------------------------------------------------------------------------

const API_BASE = "http://localhost:8080";

const pad = (n: number) => String(n).padStart(2, "0");
function toIso(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
function addDays(dateStr: string, days: number): string {
  const d = new Date(`${dateStr}T00:00:00`);
  d.setDate(d.getDate() + days);
  return toIso(d);
}

let token: string;
let pedidoNumero: string;

// fetch nativo de Node (el test corre en Node). body se omite en GET.
async function apiJson<T>(method: "GET" | "POST", path: string, body?: unknown): Promise<T> {
  const res = await fetch(API_BASE + path, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!res.ok) {
    // Log del body para debug cuando un paso de API falla.
    console.log(`[api] ${method} ${path} -> ${res.status()}: ${await res.text()}`);
    throw new Error(`API ${method} ${path} falló con status ${res.status()}`);
  }
  return (await res.json()) as T;
}

test.describe("Turno / flujo repartidor", () => {
  test.beforeAll(async () => {
    // Login admin -> token + vendedorId (usuarioId del login).
    const adminLogin = await apiJson<{ token: string; usuarioId: number }>(
      "POST",
      "/api/auth/login",
      { email: "admin@pedidos.com", password: "admin123" }
    );
    token = adminLogin.token;
    const vendedorId = adminLogin.usuarioId;

    // repartidorId (GET /usuarios?q=repartidor).
    const rep = await apiJson<{ content: { id: number }[] }>("GET", "/api/usuarios?q=repartidor");
    const repartidorId = rep.content[0].id;

    // Cliente "Cliente Demo S.A." + su zonaId.
    const cli = await apiJson<{ content: { id: number; zonaId: number }[] }>(
      "GET",
      "/api/clientes?q=Cliente%20Demo"
    );
    const clienteId = cli.content[0].id;
    const zonaId = cli.content[0].zonaId;

    // Item HAR-000 (tiene stock).
    const it = await apiJson<{ content: { id: number }[] }>("GET", "/api/items?q=HAR");
    const itemId = it.content[0].id;

    // fechaJornada = máx fecha EN_CURSO del repartidor + 1 día (para que la
    // auto-selección de /turno elija nuestra ruta de forma determinista).
    const rutasRes = await apiJson<{ estado: string; fechaJornada: string }[]>(
      "GET",
      `/api/rutas?repartidorId=${repartidorId}`
    );
    const rutasList = Array.isArray(rutasRes)
      ? rutasRes
      : (rutasRes as unknown as { content: typeof rutasRes });
    const maxFecha = rutasList
      .filter((r) => r.estado === "EN_CURSO")
      .map((r) => r.fechaJornada)
      .filter(Boolean)
      .sort()
      .at(-1);
    const fechaJornada = maxFecha ? addDays(maxFecha, 1) : toIso(new Date());

    // Crear pedido -> confirmar -> despachar.
    const pedido = await apiJson<{ id: number; numero: string }>("POST", "/api/pedidos", {
      clienteId,
      vendedorId,
      fechaJornada,
      express: false,
      items: [{ itemId, cantidad: 2, precioUnitario: 100 }],
    });
    pedidoNumero = pedido.numero;
    await apiJson("POST", "/api/pedidos/" + pedido.id + "/confirmar", {});
    await apiJson("POST", "/api/pedidos/" + pedido.id + "/despachar", {});

    // Crear ruta y arrancarla (queda EN_CURSO y el pedido EN_VIAJE).
    const ruta = await apiJson<{ id: number }>("POST", "/api/rutas", {
      zonaId,
      repartidorId,
      fechaJornada,
      pedidoIds: [pedido.id],
      capacidadBultos: 0,
    });
    await apiJson("POST", "/api/rutas/" + ruta.id + "/iniciar", {});
  });

  test("marcar ENTREGADO un pedido del turno", async ({ page }) => {
    await login(page, REPARTIDOR);
    await page.goto("/turno");

    // La vista auto-selecciona la ruta EN_CURSO más reciente (nuestra ruta).
    const header = page.getByText(/entregados \/ .* en ruta/);
    await expect(header).toBeVisible({ timeout: 10000 });
    await expect(header).toHaveText(/0 entregados \/ 1 en ruta/);

    // El pedido creado aparece en la card actual (card + TurnoHeader visibles).
    await expect(page.getByText(pedidoNumero)).toBeVisible({ timeout: 10000 });

    // Marcar ENTREGADO con el botón estable (evita el drag del SwipeButton).
    await page.getByRole("button", { name: "Confirmar entrega" }).click();

    // El contador del header avanza y el numero desaparece de la card.
    await expect(header).toHaveText(/1 entregados \/ 0 en ruta/, { timeout: 10000 });
    await expect(page.getByText(pedidoNumero)).not.toBeVisible();
  });
});
