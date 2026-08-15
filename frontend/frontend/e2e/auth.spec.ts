import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test.describe("Autenticación", () => {
  test("login con credenciales válidas redirige al panel", async ({ page }) => {
    await page.goto("/login");
    await page.fill("#email", "admin@pedidos.com");
    await page.fill("#password", "admin123");
    await page.click('button[type="submit"]');

    await page.waitForURL("**/", { timeout: 10000 });
    await expect(page).toHaveURL(/\/$/);
    // El header muestra el email logueado
    await expect(page.getByText("admin@pedidos.com")).toBeVisible();
  });

  test("login con credenciales inválidas muestra error", async ({ page }) => {
    await page.goto("/login");
    await page.fill("#email", "admin@pedidos.com");
    await page.fill("#password", "clave-incorrecta");
    await page.click('button[type="submit"]');

    await expect(page.getByText(/credenciales inválidas/i)).toBeVisible({ timeout: 8000 });
  });

  test("el repartidor ve la navegación de rutas/turno", async ({ page }) => {
    await login(page, { email: "repartidor@pedidos.com", password: "repartidor123" });
    await expect(page.getByRole("link", { name: /turno/i })).toBeVisible();
    await expect(page.getByRole("link", { name: /rutas/i })).toBeVisible();
  });
});
