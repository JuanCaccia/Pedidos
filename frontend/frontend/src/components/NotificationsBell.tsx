"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import type { Notificacion, NoLeidasResponse } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import { IconBell } from "@/components/icons";

const LIST_LIMIT = 20;

export default function NotificationsBell() {
  const { user } = useAuth();
  const containerRef = useRef<HTMLDivElement>(null);

  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [notificaciones, setNotificaciones] = useState<Notificacion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const visible =
    user?.roles.includes("ADMINISTRATIVO") || user?.roles.includes("ENCARGADO_DEPOSITO") || false;

  const fetchCount = useCallback(async () => {
    try {
      const data = await apiGet<NoLeidasResponse>("/api/notificaciones/no-leidas");
      setCount(data.cantidad);
    } catch {
      // El conteo es complementario; el panel mostrará el error al abrirlo
    }
  }, []);

  const loadList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiGet<Notificacion[]>("/api/notificaciones");
      setNotificaciones(data.slice(0, LIST_LIMIT));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
    fetchCount();
  }, [fetchCount]);

  const toggle = useCallback(() => {
    setOpen((prev) => {
      const next = !prev;
      if (next) loadList();
      return next;
    });
  }, [loadList]);

  useEffect(() => {
    if (!visible) return;
    fetchCount();
    const interval = window.setInterval(fetchCount, 30000);
    return () => window.clearInterval(interval);
  }, [visible, fetchCount]);

  useEffect(() => {
    if (!open) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    document.addEventListener("mousedown", handlePointerDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.removeEventListener("mousedown", handlePointerDown);
    };
  }, [open]);

  if (!visible) return null;

  async function marcarLeida(notificacion: Notificacion) {
    if (notificacion.leida) return;
    setNotificaciones((prev) =>
      prev.map((n) => (n.id === notificacion.id ? { ...n, leida: true } : n))
    );
    setCount((c) => Math.max(0, c - 1));
    try {
      await apiPost(`/api/notificaciones/${notificacion.id}/leer`);
    } catch {
      setNotificaciones((prev) =>
        prev.map((n) => (n.id === notificacion.id ? { ...n, leida: false } : n))
      );
      setCount((c) => c + 1);
    }
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={toggle}
        aria-label="Notificaciones"
        aria-expanded={open}
        aria-haspopup="true"
        className="relative rounded-md p-2 text-neutral-600 transition-colors hover:bg-neutral-100 dark:text-neutral-300 dark:hover:bg-neutral-800"
      >
        <IconBell />
        {count > 0 && (
          <span className="absolute -top-0.5 -right-0.5 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-xs font-semibold text-white tabular-nums">
            {count > 99 ? "99+" : count}
          </span>
        )}
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Notificaciones"
          className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-lg border border-neutral-200 bg-white shadow-lg dark:border-neutral-800 dark:bg-neutral-900"
        >
          <div className="border-b border-neutral-200 px-4 py-2.5 text-sm font-semibold text-neutral-900 dark:border-neutral-800 dark:text-neutral-100">
            Notificaciones
          </div>
          <div className="max-h-96 overflow-y-auto">
            {loading && !error && (
              <p className="px-4 py-6 text-center text-sm text-neutral-500">Cargando...</p>
            )}
            {error && (
              <p className="px-4 py-6 text-center text-sm text-red-600 dark:text-red-400">{error}</p>
            )}
            {!loading && !error && notificaciones.length === 0 && (
              <p className="px-4 py-6 text-center text-sm text-neutral-500">Sin notificaciones</p>
            )}
            {!loading &&
              !error &&
              notificaciones.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => marcarLeida(n)}
                  className={`flex w-full flex-col gap-0.5 border-b border-neutral-100 px-4 py-3 text-left transition-colors last:border-b-0 dark:border-neutral-800 ${
                    n.leida
                      ? "hover:bg-neutral-50 dark:hover:bg-neutral-800/50"
                      : "bg-neutral-50 hover:bg-neutral-100 dark:bg-neutral-800 dark:hover:bg-neutral-700"
                  }`}
                >
                  <span className="text-sm text-neutral-800 dark:text-neutral-200">{n.mensaje}</span>
                  <span className="text-xs text-neutral-500">{formatDateTime(n.fecha)}</span>
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
