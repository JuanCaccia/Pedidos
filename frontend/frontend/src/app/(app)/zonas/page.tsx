"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, apiGet, apiPost } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { Zona } from "@/lib/types";
import ActiveBadge from "@/components/ActiveBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

export default function ZonasPage() {
  const { user } = useAuth();
  const canGestionar = user?.roles.includes("ADMINISTRATIVO") ?? false;
  const [zonas, setZonas] = useState<Zona[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [nueva, setNueva] = useState("");
  const [creando, setCreando] = useState(false);
  const [editando, setEditando] = useState<number | null>(null);
  const [nombreEdit, setNombreEdit] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiGet<Zona[]>("/api/zonas");
      setZonas(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function crear(e: React.FormEvent) {
    e.preventDefault();
    if (creando) return;
    setError(null);
    if (!nueva.trim()) {
      setError("Ingresá el nombre de la zona.");
      return;
    }
    setCreando(true);
    try {
      await apiPost("/api/zonas", { nombre: nueva.trim() });
      setNueva("");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setCreando(false);
    }
  }

  async function guardarEdicion(id: number) {
    setError(null);
    if (!nombreEdit.trim()) {
      setError("El nombre no puede estar vacío.");
      return;
    }
    try {
      await apiFetch(`/api/zonas/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nombre: nombreEdit.trim() }),
      });
      setEditando(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function desactivar(z: Zona) {
    setError(null);
    try {
      await apiFetch(`/api/zonas/${z.id}/desactivar`, { method: "PATCH" });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function reactivar(z: Zona) {
    setError(null);
    try {
      await apiFetch(`/api/zonas/${z.id}/reactivar`, { method: "PATCH" });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Zonas</h1>
      </div>

      {canGestionar && (
        <form onSubmit={crear} className="flex items-center gap-2">
          <input
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
            placeholder="Nueva zona..."
            className={`${INPUT_CLASS} w-full max-w-sm`}
          />
          <Button type="submit" disabled={creando || !nueva.trim()}>
            {creando ? "Creando..." : "Crear"}
          </Button>
        </form>
      )}

      {error && <ErrorBox message={error} />}

      {loading ? (
        <Loading />
      ) : zonas.length === 0 ? (
        <p className="text-sm text-neutral-500">No hay zonas cargadas.</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                <th className="px-4 py-3 font-medium">Nombre</th>
                <th className="px-4 py-3 font-medium">Estado</th>
                <th className="px-4 py-3 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
              {zonas.map((z) => (
                <tr key={z.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                  <td className="px-4 py-3">
                    {editando === z.id ? (
                      <input
                        value={nombreEdit}
                        onChange={(e) => setNombreEdit(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            e.preventDefault();
                            guardarEdicion(z.id);
                          }
                          if (e.key === "Escape") setEditando(null);
                        }}
                        className={INPUT_CLASS}
                        autoFocus
                      />
                    ) : (
                      <span className="font-medium text-neutral-900 dark:text-neutral-100">{z.nombre}</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <ActiveBadge activo={z.activo} />
                  </td>
                  <td className="px-4 py-3">
                    {canGestionar && (
                      <div className="flex items-center justify-end gap-2">
                        {editando === z.id ? (
                          <>
                            <Button variant="secondary" onClick={() => setEditando(null)}>
                              Cancelar
                            </Button>
                            <Button onClick={() => guardarEdicion(z.id)}>Guardar</Button>
                          </>
                        ) : (
                          <>
                            <Button
                              variant="secondary"
                              onClick={() => {
                                setError(null);
                                setEditando(z.id);
                                setNombreEdit(z.nombre);
                              }}
                            >
                              Renombrar
                            </Button>
                            {z.activo ? (
                              <button
                                onClick={() => desactivar(z)}
                                className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 dark:border-neutral-700 dark:text-red-400 dark:hover:bg-red-950/40"
                              >
                                Desactivar
                              </button>
                            ) : (
                              <button
                                onClick={() => reactivar(z)}
                                className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-emerald-700 transition-colors hover:bg-emerald-50 dark:border-neutral-700 dark:text-emerald-400 dark:hover:bg-emerald-950/40"
                              >
                                Reactivar
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
