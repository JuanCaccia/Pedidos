import type { ApiError } from "@/lib/types";

export const TOKEN_KEY = "pedidos_token";
export const USER_KEY = "pedidos_user";

async function parseErrorBody(res: Response): Promise<ApiError | null> {
  try {
    return (await res.json()) as ApiError;
  } catch {
    return null;
  }
}

function clearSession(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers);
  headers.set("Accept", "application/json");

  if (typeof window !== "undefined") {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(path, { ...options, headers });
  } catch {
    throw new Error("No se pudo conectar con el servidor. Verificá que el backend esté corriendo.");
  }

  if (response.status === 401) {
    clearSession();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("Sesión expirada. Iniciá sesión nuevamente.");
  }

  if (!response.ok) {
    const errorBody = await parseErrorBody(response);
    const message = errorBody?.message?.trim() || `Error inesperado (${response.status})`;
    const error = new Error(message) as Error & { apiError?: ApiError };
    if (errorBody) error.apiError = errorBody;
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return apiFetch<T>(path);
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: "POST",
    headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

export async function apiDownloadText(path: string): Promise<string> {
  const headers = new Headers();
  headers.set("Accept", "text/csv");
  if (typeof window !== "undefined") {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(path, { headers });
  if (res.status === 401) {
    clearSession();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new Error("Sesión expirada. Iniciá sesión nuevamente.");
  }
  if (!res.ok) throw new Error("Error al exportar (" + res.status + ")");
  return res.text();
}
