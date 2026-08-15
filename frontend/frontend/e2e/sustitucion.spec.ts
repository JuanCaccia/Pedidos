import { expect, test } from "@playwright/test";
import type { APIRequestContext, Locator, Page } from "@playwright/test";
import { login } from "./helpers";

const API_BASE = "http://localhost:8080/api";

// ---------- Tipos mínimos (solo los campos que usamos) ----------
interface PageContent<T> {
  content: T[];
  totalPages: number;
}
interface ItemDTO {
  id: number;
  sku: string;
  nombre: string;
  unidadMedida: string;
  stockMinimo: string | null;
  precioLista: string | null;
  categoria: string | null;
  activo: boolean;
}
interface ClienteDTO {
  id: number;
  razonSocial: string;
  zonaId: number;
}
interface UsuarioDTO {
  id: number;
  nombre: string;
  email: string;
}
interface PedidoItemDTO {
  pedidoItemId: number;
  itemId: number;
  cantidadPedida: string;
  cantidadEntregada: string;
}
interface PedidoDTO {
  id: number;
  numero: string;
  clienteId: number;
  estado: string;
  items: PedidoItemDTO[];
}
interface LoginDTO {
  token: string;
  usuarioId: number;
}
interface CobranzaDTO {
  id: number;
  clienteId: number;
  pedidoId: number;
  monto: string;
  observaciones: string | null;
}

// ---------- Cliente HTTP con auth ----------
function authed(api: APIRequestContext, token: string) {
  return {
    get: <T>(path: string) => apiGet<T>(api, token, path),
    post: <T>(path: string, body?: unknown) => apiPost<T>(api, token, path, body),
    put: <T>(path: string, body: unknown) => apiPut<T>(api, token, path, body),
  };
}

async function apiGet<T>(api: APIRequestContext, token: string, path: string): Promise<T> {
  const res = await api.get(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`GET ${path} -> ${res.status()}: ${body}`);
  }
  return (await res.json()) as T;
}

async function apiPost<T>(api: APIRequestContext, token: string, path: string, body?: unknown): Promise<T> {
  const res = await api.post(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    data: body,
  });
  if (!res.ok()) {
    const bodyText = await res.text();
    throw new Error(`POST ${path} -> ${res.status()}: ${bodyText}`);
  }
  return (await res.json()) as T;
}

async function apiPut<T>(api: APIRequestContext, token: string, path: string, body: unknown): Promise<T> {
  const res = await api.put(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    data: body,
  });
  if (!res.ok()) {
    const bodyText = await res.text();
    throw new Error(`PUT ${path} -> ${res.status()}: ${bodyText}`);
  }
  return (await res.json()) as T;
}

// ---------- Selector del Combobox (patrón existente en pedidos.spec) ----------
async function pickCombobox(root: Locator, placeholder: string, query: string) {
  const input = root.getByPlaceholder(placeholder).first();
  await input.click();
  await input.fill(query);
  // El Combobox debouncea la búsqueda (300ms); esperamos la opción que coincida
  // con el texto buscado en lugar de tomar la primera (evita seleccionar un item stale).
  const listbox = root.getByRole("listbox").last();
  const option = listbox.getByRole("option").filter({ hasText: query }).first();
  await option.click({ timeout: 8000 });
}

// ---------- Setup del estado previo vía API (pedido ENTREGADO) ----------
async function setupPedidoEntregado(api: APIRequestContext, admin: LoginDTO) {
  const c = authed(api, admin.token);

  // 1) Setear precios distintos: HAR-000 -> 100, ACE-1L -> 50
  const har = (await c.get<PageContent<ItemDTO>>("/items?q=HAR-000&size=20")).content[0];
  const ace = (await c.get<PageContent<ItemDTO>>("/items?q=ACE-1L&size=20")).content[0];
  if (!har || !ace) throw new Error("No se resolvieron los items HAR-000 / ACE-1L");

  const actualizarPrecio = (it: ItemDTO, precio: number) =>
    c.put<ItemDTO>(`/items/${it.id}`, {
      nombre: it.nombre,
      unidadMedida: it.unidadMedida,
      stockMinimo: it.stockMinimo,
      precioLista: String(precio),
      categoria: it.categoria,
    });
  await actualizarPrecio(har, 100);
  await actualizarPrecio(ace, 50);

  // 2) Cliente y repartidor
  const cliente = (await c.get<PageContent<ClienteDTO>>("/clientes?q=Cliente%20Demo&size=20")).content[0];
  if (!cliente) throw new Error("No se resolvió el cliente Cliente Demo");
  const repartidor = (await c.get<PageContent<UsuarioDTO>>("/usuarios?q=repartidor&size=20")).content[0];
  if (!repartidor) throw new Error("No se resolvió un repartidor");

  // 3) Crear pedido con HAR (cantidad 5, precioUnitario 0) -> PENDIENTE_CONFIRMACION
  const creado = await c.post<PedidoDTO>("/pedidos", {
    clienteId: cliente.id,
    vendedorId: admin.usuarioId,
    items: [{ itemId: har.id, cantidad: 5, precioUnitario: 0 }],
  });
  const pedidoId = creado.id;
  const linea = creado.items.find((it) => it.itemId === har.id);
  if (!linea) throw new Error(`Pedido ${pedidoId} no contiene el item HAR: ${JSON.stringify(creado.items)}`);

  // 4) confirmar -> PENDIENTE_PREPARACION; despachar -> PENDIENTE_ENTREGA
  await c.post(`/pedidos/${pedidoId}/confirmar`);
  await c.post(`/pedidos/${pedidoId}/despachar`);

  // 5) Crear ruta y empezar viaje -> pedido EN_VIAJE
  const hoy = new Date().toISOString().slice(0, 10);
  const ruta = await c.post<{ id: number }>("/rutas", {
    zonaId: cliente.zonaId,
    repartidorId: repartidor.id,
    fechaJornada: hoy,
    pedidoIds: [pedidoId],
    capacidadBultos: 0,
  });
  await c.post(`/rutas/${ruta.id}/iniciar`);

  // 6) Registrar entrega total -> ENTREGADO
  await c.post<PedidoDTO>(`/pedidos/${pedidoId}/entregas`, {
    entregas: [{ pedidoItemId: linea.pedidoItemId, cantidadEntregada: 5 }],
  });

  return { pedidoId, numero: creado.numero, clienteId: cliente.id, itemOriginalId: har.id, itemSustitutoId: ace.id };
}

// ---------- Buscar la fila del pedido (paginando) en el tab "Entregado" ----------
async function localizarFilaEntregado(page: Page, numero: string): Promise<Locator> {
  const tab = page.getByRole("button", { name: "Entregado" });
  await tab.click();

  for (let i = 0; i < 10; i++) {
    const fila = page.locator("tr").filter({ hasText: numero });
    if ((await fila.count()) > 0) return fila.first();

    const siguiente = page.getByRole("button", { name: "Siguiente" });
    if (await siguiente.isDisabled()) break;
    await siguiente.click();
    await page.waitForTimeout(300);
  }
  throw new Error(`No se encontró la fila del pedido ${numero} en el tab Entregado`);
}

test.describe("Sustitución en destino", () => {
  test("reemplaza un item entregado y genera la cobranza compensatoria", async ({ page, request }) => {
    // Setup: token admin por API
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: "admin@pedidos.com", password: "admin123" },
    });
    if (!loginRes.ok()) {
      const body = await loginRes.text();
      throw new Error(`Login API falló -> ${loginRes.status()}: ${body}`);
    }
    const admin = (await loginRes.json()) as LoginDTO;

    const { numero, clienteId, itemOriginalId, itemSustitutoId } = await setupPedidoEntregado(request, admin);

    // UI: login admin e ir al detalle del pedido entregado
    await login(page);
    await page.goto("/pedidos");
    const fila = await localizarFilaEntregado(page, numero);
    await fila.getByRole("button", { name: "Detalle" }).click();

    const drawer = page.getByRole("dialog", { name: /^Pedido / });
    await expect(drawer).toBeVisible();
    await expect(drawer).toHaveAttribute("aria-modal", "false");

    // Abrir modal "Sustituir item"
    await drawer.getByRole("button", { name: "Sustituir item" }).click();
    const modal = page.getByRole("dialog", { name: /Sustituir item/ });
    await expect(modal).toBeVisible();
    await expect(modal).toHaveAttribute("aria-modal", "true");

    // Item original: select nativo por value=itemId de HAR
    await modal.locator("#sustitucion-item-original").selectOption({ value: String(itemOriginalId) });

    // Item sustituto: Combobox (patrón listbox)
    await pickCombobox(modal, "Buscar item...", "ACE-1L");

    // Cantidad y observaciones
    await modal.locator("#sustitucion-cantidad").fill("5");
    await modal.locator("#sustitucion-observaciones").fill("Sustitución E2E de prueba");

    // Submit
    await modal.getByRole("button", { name: "Registrar sustitución" }).click();

    // Éxito: banner status con el número del pedido (el modal se cierra)
    await expect(page.getByRole("status")).toContainText(numero, { timeout: 10000 });
    await expect(modal).not.toBeVisible();

    // Verificar cobranza compensatoria vía API
    const pedidoId = await pedidoIdDe(request, admin.token, numero);
    const cobranzas = await authed(request, admin.token).get<CobranzaDTO[]>(
      `/cobranzas?clienteId=${clienteId}`
    );
    const cobranza = cobranzas.find(
      (cb) => cb.pedidoId === pedidoId && /Sustitución pedido/.test(cb.observaciones ?? "")
    );
    expect(cobranza, `No se encontró la cobranza de sustitución para el pedido ${numero}. Cobranzas: ${JSON.stringify(cobranzas)}`).toBeTruthy();
    // monto esperado = (100 - 50) * 5 = 250
    expect(Number(cobranza!.monto)).toBe(250);
  });
});

// Resuelve el id del pedido por número para verificar la cobranza
async function pedidoIdDe(api: APIRequestContext, token: string, numero: string): Promise<number> {
  const c = authed(api, token);
  for (let page = 0; page < 10; page++) {
    const data = await c.get<PageContent<PedidoDTO>>(`/pedidos?page=${page}&size=100`);
    const match = data.content.find((p) => p.numero === numero);
    if (match) return match.id;
    if (page >= data.totalPages - 1) break;
  }
  throw new Error(`No se resolvió el id del pedido ${numero}`);
}
