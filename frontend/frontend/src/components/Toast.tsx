"use client";

import { createContext, useCallback, useContext, useRef, useState } from "react";
import type { ReactNode } from "react";
import { IconClose } from "@/components/icons";

type ToastType = "success" | "error";

interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  success: (message: string) => void;
  error: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const DURATION_MS = 4000;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(0);

  const remove = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (type: ToastType, message: string) => {
      const id = ++nextId.current;
      setToasts((prev) => [...prev, { id, message, type }]);
      window.setTimeout(() => remove(id), DURATION_MS);
    },
    [remove]
  );

  const success = useCallback((message: string) => push("success", message), [push]);
  const error = useCallback((message: string) => push("error", message), [push]);

  return (
    <ToastContext.Provider value={{ success, error }}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2">
        {toasts.map((t) => (
          <ToastCard key={t.id} toast={t} onClose={() => remove(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastCard({ toast, onClose }: { toast: ToastItem; onClose: () => void }) {
  const styles =
    toast.type === "error"
      ? "border-red-200 bg-red-50 text-red-800 dark:border-red-900/50 dark:bg-red-950/60 dark:text-red-200"
      : "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/60 dark:text-emerald-200";

  return (
    <div
      role={toast.type === "error" ? "alert" : "status"}
      className={`pointer-events-auto flex items-start justify-between gap-3 rounded-md border px-4 py-3 text-sm font-medium shadow-lg ${styles}`}
    >
      <span>{toast.message}</span>
      <button
        type="button"
        onClick={onClose}
        aria-label="Cerrar notificación"
        className="shrink-0 opacity-60 transition-opacity hover:opacity-100"
      >
        <IconClose />
      </button>
    </div>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast debe usarse dentro de un <ToastProvider>");
  }
  return ctx;
}
