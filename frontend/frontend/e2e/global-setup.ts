const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

const MAX_ATTEMPTS = 10;
const RETRY_DELAY_MS = 2000;

async function globalSetup(): Promise<void> {
  const url = `${API_BASE_URL}/api/test/reset`;

  for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
    try {
      const res = await fetch(url, { method: "POST" });
      if (res.ok) {
        console.log(`[globalSetup] DB reset OK (${url})`);
        return;
      }
      const body = await res.text();
      throw new Error(`[globalSetup] reset respondio ${res.status}: ${body}`);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      if (attempt === MAX_ATTEMPTS) {
        throw new Error(
          `[globalSetup] no se pudo resetear la DB luego de ${MAX_ATTEMPTS} intentos (${url}): ${message}`,
        );
      }
      console.warn(
        `[globalSetup] intento ${attempt}/${MAX_ATTEMPTS} fallo, reintento en ${RETRY_DELAY_MS}ms: ${message}`,
      );
      await new Promise((resolve) => setTimeout(resolve, RETRY_DELAY_MS));
    }
  }
}

export default globalSetup;
