import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Dashboard accionable", () => {
  test("el link 'Lotes por vencer' navega a /stock pre-filtrado por vencer", async ({ page }) => {
    await login(page);

    // El card del dashboard "Lotes por vencer" tiene un link "Ver stock"
    const card = page.locator("section", { hasText: "Lotes por vencer" });
    await card.getByRole("link", { name: "Ver stock" }).click();

    await expect(page).toHaveURL(/\/stock\?tab=lotes&filtro=vencer/);

    // La sección de Lotes está visible con el filtro "Por vencer" activo
    const seccionLotes = page.getByRole("heading", { name: "Lotes" });
    await expect(seccionLotes).toBeVisible();

    const chipPorVencer = page.getByRole("button", { name: /Por vencer/ });
    await expect(chipPorVencer).toHaveAttribute("aria-pressed", "true");
  });
});
