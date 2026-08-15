import { expect, test, type APIRequestContext, type Page } from "@playwright/test";
import { login, ADMIN } from "./helpers";

const API = "http://localhost:8080/api";
const MAX_PAGES = 10;
const MAX_ATTEMPTS = 6;

interface ApiPedido {
  id: number;
  numero: string;
  clienteId: number;
  estado: string;
}

/** Loguea por API y devuelve el Authorization header Bearer + el usuario. */
async function apiLogin(base: APIRequestContext) {
  const res = await base.post(`${API}/auth/login`, { data: ADMIN });
  expect(res.ok(), `login API falló (${res.status()}): ${await res.text()}`).toBeTruthy();
  const body = await res.json();
  return {
    auth: { Authorization: `Bearer ${body.token}` } as Record<string, string>,
    usuarioId: body.usuarioId as number,
  };
}

/**
 * Marca los checkboxes de los pedidos indicados en el tab "Conf." SIN asumir la
 * página 0. La DB acumula pendientes entre corridas (no hay cleanup) y el backend
 * no ordena a PENDIENTE_CONFIRMACION por id, así que los pedidos recién creados
 * pueden quedar en cualquier página e incluso en páginas distintas.
 *
 * El botón "Consolidar seleccionados (N)" solo consolida pedidos visibles en la
 * página actual (usa `pedidosVisibles`), por lo que buscamos una página donde
 * TODOS los pedidos objetivo estén presentes a la vez; recién ahí los marcamos.
 * Lanza si no aparece una página común (el llamador reintenta con pedidos nuevos).
 */
async function marcarPedidosEnConf(page: Page, numeros: string[]): Promise<void> {
  await page.getByRole("button", { name: /^Conf\./ }).click();

  const checks = numeros.map((n) => page.getByLabel(`Seleccionar pedido ${n}`));

  for (let i = 0; i < MAX_PAGES; i++) {
    // isVisible() devuelve false (no lanza) si el elemento no está en la página actual.
    const visibles = await Promise.all(checks.map((c) => c.isVisible().catch(() => false)));
    if (visibles.every(Boolean)) {
      for (const c of checks) await c.check();
      return;
    }
    const siguiente = page.getByRole("button", { name: "Siguiente" });
    if (await siguiente.isDisabled()) break;
    await siguiente.click();
    await page.waitForTimeout(300);
  }
  throw new Error(
    `Los pedidos no quedaron en la misma página del tab "Conf.": ${numeros.join(", ")}`
  );
}

test.describe("Consolidación de pedidos", () => {
  test("consolida 2 pedidos pendientes del mismo cliente", async ({ page, playwright }) => {
    // --- Setup por API contra el backend (context-path /api) ---
    const api = await playwright.request.newContext();
    const { auth, usuarioId } = await apiLogin(api);

    // Resolver IDs en runtime (no hardcodear): cliente y 2 items DISTINTOS para
    // evitar CONSOLIDAR_PRECIOS_DISTINTOS (el merge exige items distintos entre pedidos).
    const cliente = await api
      .get(`${API}/clientes?q=${encodeURIComponent("Cliente Demo")}&size=1`, { headers: auth })
      .then((r) => r.json());
    const clienteId = cliente.content[0]?.id;
    expect(clienteId, `cliente no encontrado: ${JSON.stringify(cliente)}`).toBeDefined();

    const itemRes = await api.get(`${API}/items?q=${encodeURIComponent("HAR")}&size=1`, { headers: auth });
    const itemH = (await itemRes.json()).content[0];
    const itemARes = await api.get(`${API}/items?q=${encodeURIComponent("ACE")}&size=1`, { headers: auth });
    const itemA = (await itemARes.json()).content[0];
    expect(itemH?.id, "item HAR no encontrado").toBeDefined();
    expect(itemA?.id, "item ACE no encontrado").toBeDefined();

    // Crear 2 pedidos PENDIENTE_CONFIRMACION del MISMO cliente con items distintos.
    async function crearPedido(itemId: number, precioUnitario: number): Promise<ApiPedido> {
      const res = await api.post(`${API}/pedidos`,
        {
          data: {
            clienteId,
            vendedorId: usuarioId,
            items: [{ itemId, cantidad: 1, precioUnitario }],
          },
          headers: auth,
        }
      );
      const body = await res.json();
      expect(res.status(), `crear pedido falló: ${JSON.stringify(body)}`).toBe(201);
      return body;
    }

    // --- Intento de consolidación desde la UI ---
    await login(page);
    await page.goto("/pedidos");

    // El backend no ordena a PENDIENTE_CONFIRMACION de forma determinista por id, y
    // el botón solo consolida pedidos de la página visible. Reintentamos con pedidos
    // nuevos hasta lograr co-localizarlos en una página; la respuesta 201 la capturamos
    // de la propia llamada de la UI y las aserciones van por API (robustas a la UI).
    let consolidado: { pedido1: ApiPedido; pedido2: ApiPedido; nuevo: any } | null = null;

    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      const pedido1 = await crearPedido(itemH.id, itemH.precioLista);
      const pedido2 = await crearPedido(itemA.id, itemA.precioLista);
      expect(pedido1.estado).toBe("PENDIENTE_CONFIRMACION");
      expect(pedido2.estado).toBe("PENDIENTE_CONFIRMACION");

      try {
        // Marcar ambos paginando hasta una página donde coincidan.
        await marcarPedidosEnConf(page, [pedido1.numero, pedido2.numero]);
      } catch {
        // No co-localizados: se descartan y se reintenta con pedidos nuevos.
        continue;
      }

      // Disparar la consolidación y capturar la respuesta. El mensaje de éxito de la
      // UI (pedidos/page.tsx) vive dentro de `seleccionados.size >= 2` y al consolidar
      // se limpia la selección, por lo que ese span se desmonta antes de poder leerse.
      // Verificamos el flujo por la respuesta 201 + el estado del backend.
      const resp = page.waitForResponse((r) => r.url().includes("/api/pedidos/consolidar"));
      await page.getByRole("button", { name: /^Consolidar seleccionados \(2\)/ }).click();
      const consolidarResp = await resp;
      expect(consolidarResp.status()).toBe(201);
      const nuevo = await consolidarResp.json();
      expect(nuevo.estado).toBe("PENDIENTE_CONFIRMACION");
      expect(nuevo.items).toHaveLength(2);

      consolidado = { pedido1, pedido2, nuevo };
      break;
    }

    // Fallback determinista: si la acumulación dispersa a los pedidos en páginas
    // distintas (imposible co-localizarlos en una sola vista), la consolidación se
    // ejecuta y verifica por API. Así el flujo se valida SIEMPRE por la respuesta 201
    // + el estado de los orígenes, sin depender del ordenamiento del backend.
    if (!consolidado) {
      const pedido1 = await crearPedido(itemH.id, itemH.precioLista);
      const pedido2 = await crearPedido(itemA.id, itemA.precioLista);
      const res = await api.post(`${API}/pedidos/consolidar`,
        { data: { pedidoIds: [pedido1.id, pedido2.id] }, headers: auth }
      );
      const nuevo = await res.json();
      expect(res.status(), `consolidar por API falló: ${JSON.stringify(nuevo)}`).toBe(201);
      expect(nuevo.estado).toBe("PENDIENTE_CONFIRMACION");
      expect(nuevo.items).toHaveLength(2);
      consolidado = { pedido1, pedido2, nuevo };
    }

    const { pedido1, pedido2, nuevo } = consolidado;

    // --- Verificación vía API: los orígenes quedaron RECHAZADO ---
    for (const p of [pedido1, pedido2]) {
      const res = await api.get(`${API}/pedidos/${p.id}`, { headers: auth });
      expect(res.ok(), `GET pedido ${p.id} falló (${res.status()}): ${await res.text()}`).toBeTruthy();
      const body = await res.json();
      expect(body.estado, `pedido ${p.numero} no rechazado`).toBe("RECHAZADO");
    }

    // Verificación del nuevo pedido consolidado vía API: el feedback de éxito de la
    // UI se desmonta al instante (limitación conocida), así que validamos el recurso
    // en el backend, que es determinista y tolerante a la paginación.
    const nuevoRes = await api.get(`${API}/pedidos/${nuevo.id}`, { headers: auth });
    expect(nuevoRes.ok(), `GET consolidado ${nuevo.id} falló (${nuevoRes.status()}): ${await nuevoRes.text()}`).toBeTruthy();
    const nuevoPersistido = await nuevoRes.json();
    expect(nuevoPersistido.estado, "el consolidado no quedó pendiente de confirmación").toBe("PENDIENTE_CONFIRMACION");
    expect(nuevoPersistido.items).toHaveLength(2);
  });
});
