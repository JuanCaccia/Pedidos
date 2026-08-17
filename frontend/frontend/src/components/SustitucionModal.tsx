"use client";

import { useMemo, useState } from "react";
import type { Item, Pedido, PageResponse, Sustitucion } from "@/lib/types";
import { apiGet, apiPost } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import Button from "@/components/Button";
import Combobox from "@/components/Combobox";
import ErrorBox from "@/components/ErrorBox";
import Modal from "@/components/Modal";

export default function SustitucionModal({
  pedido,
  onClose,
  onConfirm,
}: {
  pedido: Pedido;
  onClose: () => void;
  onConfirm: () => Promise<void> | void;
}) {
  const isEnViaje = pedido.estado === "EN_VIAJE";

  const lineasSustituibles = useMemo(() => {
    if (isEnViaje) {
      return pedido.items.filter((it) => it.cantidadReservada > 0);
    }
    return pedido.items.filter((it) => it.cantidadEntregada > 0);
  }, [isEnViaje, pedido.items]);

  const [itemOriginalId, setItemOriginalId] = useState<number | null>(
    lineasSustituibles[0]?.itemId ?? null
  );
  const [itemSustitutoId, setItemSustitutoId] = useState<number | null>(null);
  const [cantidad, setCantidad] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const lineaOriginal =
    lineasSustituibles.find((it) => it.itemId === itemOriginalId) ?? null;
  const cantidadMax = isEnViaje
    ? (lineaOriginal?.cantidadReservada ?? 0)
    : (lineaOriginal?.cantidadEntregada ?? 0);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (itemOriginalId == null) {
      setError("Seleccioná el item original.");
      return;
    }
    if (itemSustitutoId == null) {
      setError("Seleccioná el item sustituto.");
      return;
    }
    if (itemSustitutoId === itemOriginalId) {
      setError("El item sustituto debe ser distinto del item entregado.");
      return;
    }
    const cantidadNum = Number(cantidad);
    if (!cantidad || Number.isNaN(cantidadNum) || cantidadNum <= 0) {
      setError("Ingresá una cantidad mayor a cero.");
      return;
    }
    if (cantidadNum > cantidadMax) {
      setError(`La cantidad no puede superar el item original (${formatNumber(cantidadMax)}).`);
      return;
    }

    setSubmitting(true);
    try {
      await apiPost<Sustitucion>("/api/sustituciones", {
        pedidoId: pedido.id,
        itemOriginalId,
        itemSustitutoId,
        cantidad: cantidadNum,
        observaciones: observaciones.trim() || undefined,
      });
      await onConfirm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={`Sustituir item · pedido ${pedido.numero}`} onClose={onClose} width="md">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="sustitucion-item-original"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            {isEnViaje ? "Item del pedido" : "Item entregado"}
          </label>
          <select
            id="sustitucion-item-original"
            value={itemOriginalId ?? ""}
            onChange={(e) => {
              setItemOriginalId(e.target.value ? Number(e.target.value) : null);
              setCantidad("");
            }}
            required
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          >
            {lineasSustituibles.length === 0 && (
              <option value="">
                {isEnViaje ? "Sin items reservados" : "Sin items entregados"}
              </option>
            )}
            {lineasSustituibles.map((it) => (
              <option key={it.pedidoItemId} value={it.itemId}>
                #{it.itemId} — {formatNumber(isEnViaje ? it.cantidadReservada : it.cantidadEntregada)}{" "}
                {isEnViaje ? "reservada" : "entregada"}
              </option>
            ))}
          </select>
        </div>

        <Combobox
          label="Item correcto (sustituto)"
          placeholder="Buscar item..."
          required
          value={itemSustitutoId}
          onChange={setItemSustitutoId}
          search={async (q) => {
            const data = await apiGet<PageResponse<Item>>(
              `/api/items?q=${encodeURIComponent(q)}&size=20&activos=true`
            );
            return data.content.map((i) => ({
              id: i.id,
              label: `${i.sku} — ${i.nombre}`,
              sublabel: i.categoriaNombre ?? "",
            }));
          }}
        />

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="sustitucion-cantidad"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            Cantidad (máx. {formatNumber(cantidadMax)})
          </label>
          <input
            id="sustitucion-cantidad"
            type="number"
            min="0.001"
            max={cantidadMax || undefined}
            step="0.001"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="sustitucion-observaciones"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            Observaciones
          </label>
          <textarea
            id="sustitucion-observaciones"
            value={observaciones}
            onChange={(e) => setObservaciones(e.target.value)}
            rows={3}
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          />
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting || lineasSustituibles.length === 0}>
            {submitting ? "Guardando..." : "Registrar sustitución"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
