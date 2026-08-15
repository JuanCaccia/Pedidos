"use client";

import { useState } from "react";
import Modal from "@/components/Modal";
import Button from "@/components/Button";
import ErrorBox from "@/components/ErrorBox";

interface ConfirmacionFrictionadaProps {
  title: string;
  descripcion: string;
  palabra: string;
  confirmLabel: string;
  onConfirm: () => Promise<void> | void;
  onClose: () => void;
}

export default function ConfirmacionFrictionada({
  title,
  descripcion,
  palabra,
  confirmLabel,
  onConfirm,
  onClose,
}: ConfirmacionFrictionadaProps) {
  const [input, setInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const confirmado = input === palabra;

  async function handleConfirm() {
    if (!confirmado || saving) return;
    setSaving(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-400">{descripcion}</p>
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="confirmacion-frictionada"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            Escriba "{palabra}" para confirmar
          </label>
          <input
            id="confirmacion-frictionada"
            type="text"
            autoComplete="off"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          />
        </div>
        {error && <ErrorBox message={error} />}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Volver
          </Button>
          <Button
            type="button"
            onClick={handleConfirm}
            disabled={!confirmado || saving}
            className="bg-red-600 text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? "Procesando..." : confirmLabel}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
