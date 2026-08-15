import { expect, Page } from "@playwright/test";

export const ADMIN = { email: "admin@pedidos.com", password: "admin123" };
export const REPARTIDOR = { email: "repartidor@pedidos.com", password: "repartidor123" };

/** Loguea por la UI y espera la redirección al panel. */
export async function login(page: Page, user = ADMIN) {
  await page.goto("/login");
  await page.fill("#email", user.email);
  await page.fill("#password", user.password);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/", { timeout: 10000 });
  await expect(page).toHaveURL(/\/$/);
}
