import { expect, test } from "@playwright/test";
import type { Locator } from "@playwright/test";
import { login } from "./helpers";

async function pickCombobox(drawer: Locator, placeholder: string, query: string) {
  const input = drawer.getByPlaceholder(placeholder).first();
  await input.click();
  await input.fill(query);
  const listbox = drawer.getByRole("listbox").last();
  const option = listbox.getByRole("option").first();
  await option.click({ timeout: 8000 });
}

test.describe("Pedidos", () => {
  test("crear un pedido express y verlo en la lista", async ({ page }) => {
    await login(page);
    await page.goto("/pedidos");
    await expect(page.getByRole("heading", { name: "Pedidos" })).toBeVisible();

    await page.getByRole("button", { name: "Nuevo pedido" }).click();
    const drawer = page.getByRole("dialog", { name: "Nuevo pedido" });
    await expect(drawer).toBeVisible();

    await pickCombobox(drawer, "Buscar cliente...", "Cliente Demo");
    await pickCombobox(drawer, "Buscar item...", "Harina");

    // Cantidad
    await drawer.getByPlaceholder("Cant.").first().fill("2");

    // Flag express (checkbox directo dentro del drawer)
    await drawer.locator('input[type="checkbox"]').first().check();

    await drawer.getByRole("button", { name: "Crear pedido" }).click();

    // El form se cierra tras crear. El nuevo pedido queda en PENDIENTE_CONFIRMACION,
    // así que vamos al tab "Conf." para localizarlo (la lista "Todos" no reordena en caliente).
    await expect(drawer).not.toBeVisible({ timeout: 10000 });
    await page.getByRole("button", { name: /^Conf\./ }).click();
    await expect(page.getByText("Express").first()).toBeVisible({ timeout: 10000 });
  });
});
