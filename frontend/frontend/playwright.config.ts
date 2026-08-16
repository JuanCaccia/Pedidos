import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  globalSetup: "./e2e/global-setup.ts",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30000,
  expect: { timeout: 8000 },
  reporter: [["list"]],
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: {
    // La app ya corre en containers (frontend :3000, backend :8080). Solo espera a que responda.
    command: "sleep 2",
    url: "http://localhost:3000/login",
    reuseExistingServer: true,
    timeout: 30000,
  },
});
