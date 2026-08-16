"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import type { Item, Lote, MovimientoStock, PageResponse, ReporteStockItem } from "@/lib/types";
import { formatDate, formatDateTime, formatNumber } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Pagination from "@/components/Pagination";
import { exportarCSV } from "@/lib/export";

const MOV_PAGE_SIZE = 20;

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

type StockModal = "ingreso" | "merma" | "ajuste" | null;

export default function StockPage() {
  const { user } = useAuth();
  const canOperate =
    user?.roles.some((r) => r === "ENCARGADO_DEPOSITO" || r === "ADMINISTRATIVO") ?? false;

  const [stock, setStock] = useState<ReporteStockItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [lotes, setLotes] = useState<Lote[]>([]);
  const [lotesLoading, setLotesLoading] = useState(true);
  const [lotesError, setLotesError] = useState<string | null>(null);
  const [lotesFilter, setLotesFilter] = useState<"todos" | "por-vencer" | "vencidos" | "agotados">("todos");

  const [selected, setSelected] = useState<ReporteStockItem | null>(null);
  const [movimientos, setMovimientos] = useState<MovimientoStock[]>([]);
  const [movLoading, setMovLoading] = useState(false);
  const [movError, setMovError] = useState<string | null>(null);
  const [movPage, setMovPage] = useState(0);
  const [movTotalPages, setMovTotalPages] = useState(1);

  const [items, setItems] = useState<Item[]>([]);

  const [modal, setModal] = useState<StockModal>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const loadStock = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setStock(await apiGet<ReporteStockItem[]>("/api/reportes/stock"));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStock();
  }, [loadStock]);

  useEffect(() => {
    let cancelled = false;
    setLotesLoading(true);
    setLotesError(null);
    apiGet<Lote[]>("/api/stock/lotes")
      .then((data) => {
        if (!cancelled) setLotes(data);
      })
      .catch((err) => {
        if (!cancelled) setLotesError(err instanceof Error ? err.message : "Error inesperado");
      })
      .finally(() => {
        if (!cancelled) setLotesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!canOperate) return;
    let cancelled = false;
    apiGet<PageResponse<Item>>("/api/items?size=500")
      .then((data) => {
        if (!cancelled) setItems(data.content);
      })
      .catch(() => {
        // El formulario mostrará el error al intentar usarlo
      });
    return () => {
      cancelled = true;
    };
  }, [canOperate]);

  useEffect(() => {
    if (!selected) {
      setMovimientos([]);
      setMovError(null);
      setMovTotalPages(1);
      setMovPage(0);
      return;
    }
    setMovPage(0);
  }, [selected]);

  useEffect(() => {
    if (!selected) return;
    let cancelled = false;
    setMovLoading(true);
    setMovError(null);
    const params = new URLSearchParams({ page: String(movPage), size: String(MOV_PAGE_SIZE) });
    apiGet<PageResponse<MovimientoStock>>(
      `/api/stock/items/${selected.itemId}/movimientos?${params.toString()}`
    )
      .then((data) => {
        if (!cancelled) {
          setMovimientos(data.content);
          setMovTotalPages(data.totalPages);
        }
      })
      .catch((err) => {
        if (!cancelled) setMovError(err instanceof Error ? err.message : "Error inesperado");
      })
      .finally(() => {
        if (!cancelled) setMovLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selected, movPage]);

  function isLow(stockItem: ReporteStockItem): boolean {
    return stockItem.disponible <= 0 || stockItem.disponible < stockItem.reservasActivas;
  }

  const DIAS_POR_VENCER = 30;
  function filtrarLotes(): Lote[] {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const limite = new Date(hoy);
    limite.setDate(limite.getDate() + DIAS_POR_VENCER);
    return lotes.filter((l) => {
      switch (lotesFilter) {
        case "vencidos":
          return l.estado === "VENCIDO";
        case "agotados":
          return l.estado === "AGOTADO";
        case "por-vencer":
          return (
            l.fechaVencimiento !== null &&
            new Date(l.fechaVencimiento as string).getTime() <= limite.getTime()
          );
        default:
          return true;
      }
    });
  }

  function openModal(next: StockModal) {
    setSuccessMsg(null);
    setFormError(null);
    setModal(next);
  }

  async function handleCreated(message: string) {
    setSuccessMsg(message);
    setModal(null);
    setSelected(null);
    await loadStock();
  }

  async function exportar() {
    try {
      await exportarCSV("/api/reportes/stock/exportar.csv", "stock.csv");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Stock</h1>
        {canOperate && (
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => openModal("ingreso")}>
              Registrar ingreso
            </Button>
            <Button onClick={() => openModal("merma")}>
              Registrar merma
            </Button>
            <Button onClick={() => openModal("ajuste")}>
              Ajuste de inventario
            </Button>
          </div>
        )}
        <Button variant="secondary" className="px-3 py-1.5" onClick={exportar}>
          Exportar CSV
        </Button>
      </div>

      {successMsg && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
          {successMsg}
        </div>
      )}

      {error ? (
        <ErrorBox message={error} />
      ) : loading ? (
        <Loading />
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">Item ID</th>
                  <th className="px-4 py-3 font-medium">SKU</th>
                  <th className="px-4 py-3 font-medium">Nombre</th>
                  <th className="px-4 py-3 font-medium text-right">Disponible</th>
                  <th className="px-4 py-3 font-medium text-right">Reservas activas</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {stock.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay items con stock
                    </td>
                  </tr>
                )}
                {stock.map((item) => {
                  const low = isLow(item);
                  const isSelected = selected?.itemId === item.itemId;
                  return (
                    <tr
                      key={item.itemId}
                      className={`transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50 ${
                        isSelected ? "bg-blue-50 dark:bg-blue-950/40" : ""
                      }`}
                    >
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{item.itemId}</td>
                      <td className="px-4 py-3 font-mono text-neutral-700 dark:text-neutral-300">{item.sku}</td>
                      <td className="px-4 py-3 text-neutral-900 dark:text-neutral-100">{item.nombre}</td>
                      <td className="px-4 py-3 text-right">
                        <span
                          className={`font-semibold ${
                            low
                              ? "text-red-600 dark:text-red-400"
                              : "text-neutral-700 dark:text-neutral-300"
                          }`}
                        >
                          {formatNumber(item.disponible)}
                          {low && (
                            <span className="ml-2 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-900/40 dark:text-red-300">
                              bajo
                            </span>
                          )}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-neutral-700 dark:text-neutral-300">
                        {formatNumber(item.reservasActivas)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="secondary"
                          onClick={() => setSelected(isSelected ? null : item)}
                        >
                          {isSelected ? "Ocultar" : "Movimientos"}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selected && (
        <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
            <h2 className="font-medium text-neutral-900 dark:text-neutral-100">
              Movimientos · {selected.nombre} ({selected.sku})
            </h2>
            <button
              onClick={() => setSelected(null)}
              className="rounded-md px-2 py-1 text-sm text-neutral-500 hover:bg-neutral-100 dark:hover:bg-neutral-800"
            >
              Cerrar
            </button>
          </div>
          {movLoading ? (
            <Loading label="Cargando movimientos..." />
          ) : movError ? (
            <div className="p-5">
              <ErrorBox message={movError} />
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                      <th className="px-5 py-2.5 font-medium">Tipo</th>
                      <th className="px-5 py-2.5 font-medium text-right">Cantidad</th>
                      <th className="px-5 py-2.5 font-medium">Fecha</th>
                      <th className="px-5 py-2.5 font-medium">Motivo</th>
                      <th className="px-5 py-2.5 font-medium">Pedido</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                    {movimientos.length === 0 && (
                      <tr>
                        <td colSpan={5} className="px-5 py-6 text-center text-neutral-500">
                          Sin movimientos registrados
                        </td>
                      </tr>
                    )}
                    {movimientos.map((mov) => (
                      <tr key={mov.id}>
                        <td className="px-5 py-2.5">
                          <span
                            className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                              mov.cantidad < 0
                                ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"
                                : "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300"
                            }`}
                          >
                            {mov.tipo}
                          </span>
                        </td>
                        <td className="px-5 py-2.5 text-right font-medium text-neutral-700 dark:text-neutral-300">
                          {mov.cantidad > 0 ? `+${formatNumber(mov.cantidad)}` : formatNumber(mov.cantidad)}
                        </td>
                        <td className="px-5 py-2.5 text-neutral-500">{formatDateTime(mov.fecha)}</td>
                        <td className="px-5 py-2.5 text-neutral-600 dark:text-neutral-400">{mov.motivo ?? "-"}</td>
                        <td className="px-5 py-2.5 text-neutral-600 dark:text-neutral-400">{mov.pedidoId ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={movPage} totalPages={movTotalPages} onPageChange={setMovPage} />
            </>
          )}
        </section>
      )}

      <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Lotes</h2>
          <div className="flex flex-wrap gap-2 text-xs">
            {(
              [
                ["todos", "Todos"],
                ["por-vencer", `Por vencer (${DIAS_POR_VENCER} días)`],
                ["vencidos", "Vencidos"],
                ["agotados", "Agotados"],
              ] as const
            ).map(([value, label]) => (
              <button
                key={value}
                onClick={() => setLotesFilter(value)}
                className={`rounded-full px-3 py-1 font-medium transition-colors ${
                  lotesFilter === value
                    ? "bg-blue-600 text-white"
                    : "bg-neutral-100 text-neutral-600 hover:bg-neutral-200 dark:bg-neutral-800 dark:text-neutral-300 dark:hover:bg-neutral-700"
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        {lotesLoading ? (
          <Loading label="Cargando lotes..." />
        ) : lotesError ? (
          <div className="p-5">
            <ErrorBox message={lotesError} />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-5 py-2.5 font-medium">Item</th>
                  <th className="px-5 py-2.5 font-medium">Código de lote</th>
                  <th className="px-5 py-2.5 font-medium">Vencimiento</th>
                  <th className="px-5 py-2.5 font-medium text-right">Disponible</th>
                  <th className="px-5 py-2.5 font-medium">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {filtrarLotes().length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-5 py-6 text-center text-neutral-500">
                      No hay lotes para este filtro
                    </td>
                  </tr>
                )}
                {filtrarLotes().map((lote) => {
                  const estadoStyles =
                    lote.estado === "VENCIDO"
                      ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"
                      : lote.estado === "AGOTADO"
                        ? "bg-neutral-200 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-200"
                        : "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300";
                  return (
                    <tr key={lote.id}>
                      <td className="px-5 py-2.5 text-neutral-900 dark:text-neutral-100">
                        {lote.itemNombre ?? `Item #${lote.itemId}`}
                        {lote.itemSku && (
                          <span className="ml-2 font-mono text-xs text-neutral-500">{lote.itemSku}</span>
                        )}
                      </td>
                      <td className="px-5 py-2.5 font-mono text-neutral-700 dark:text-neutral-300">
                        {lote.codigoLote}
                      </td>
                      <td className="px-5 py-2.5 text-neutral-600 dark:text-neutral-400">
                        {lote.fechaVencimiento ? formatDate(lote.fechaVencimiento) : "-"}
                      </td>
                      <td className="px-5 py-2.5 text-right font-medium text-neutral-700 dark:text-neutral-300">
                        {formatNumber(lote.disponible)}
                      </td>
                      <td className="px-5 py-2.5">
                        <span
                          className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${estadoStyles}`}
                        >
                          {lote.estado}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modal === "ingreso" && (
        <IngresoForm
          items={items}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setModal(null)}
          onCreated={handleCreated}
        />
      )}

      {modal === "merma" && (
        <MermaForm
          items={items}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setModal(null)}
          onCreated={handleCreated}
        />
      )}

      {modal === "ajuste" && (
        <AjusteForm
          items={items}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setModal(null)}
          onCreated={handleCreated}
        />
      )}
    </div>
  );
}

function IngresoForm({
  items,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  items: Item[];
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: (message: string) => Promise<void>;
}) {
  const [itemId, setItemId] = useState("");
  const [codigoLote, setCodigoLote] = useState("");
  const [fechaVencimiento, setFechaVencimiento] = useState("");
  const [cantidad, setCantidad] = useState("");
  const [motivo, setMotivo] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!itemId) {
      setError("Seleccioná un item.");
      return;
    }
    const cantidadNum = Number(cantidad);
    if (!cantidad || Number.isNaN(cantidadNum) || cantidadNum <= 0) {
      setError("Ingresá una cantidad mayor a cero.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/stock/ingresos", {
        itemId: Number(itemId),
        codigoLote: codigoLote.trim() || undefined,
        fechaVencimiento: fechaVencimiento || undefined,
        cantidad: cantidadNum,
        motivo: motivo.trim() || undefined,
      });
      await onCreated("Ingreso registrado correctamente.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Registrar ingreso" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ingreso-item" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Item
          </label>
          <select
            id="ingreso-item"
            value={itemId}
            onChange={(e) => setItemId(e.target.value)}
            className={INPUT_CLASS}
          >
            <option value="">Seleccionar...</option>
            {items.map((i) => (
              <option key={i.id} value={i.id}>
                {i.sku} — {i.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ingreso-codigo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Código de lote
          </label>
          <input
            id="ingreso-codigo"
            value={codigoLote}
            onChange={(e) => setCodigoLote(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Opcional. Si queda vacío se genera automáticamente"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ingreso-vencimiento" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Fecha de vencimiento
          </label>
          <input
            id="ingreso-vencimiento"
            type="date"
            value={fechaVencimiento}
            onChange={(e) => setFechaVencimiento(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ingreso-cantidad" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Cantidad
          </label>
          <input
            id="ingreso-cantidad"
            type="number"
            min="0.001"
            step="0.001"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Ej.: 12.5"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ingreso-motivo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Motivo
          </label>
          <input
            id="ingreso-motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Opcional"
          />
        </div>

        {error && <ErrorBox message={error} />}

        <ModalActions
          submitting={submitting}
          submitLabel={submitting ? "Registrando..." : "Registrar ingreso"}
          onClose={onClose}
        />
      </form>
    </Modal>
  );
}

function MermaForm({
  items,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  items: Item[];
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: (message: string) => Promise<void>;
}) {
  const [itemId, setItemId] = useState("");
  const [loteId, setLoteId] = useState("");
  const [lotes, setLotes] = useState<Lote[]>([]);
  const [lotesLoading, setLotesLoading] = useState(false);
  const [lotesError, setLotesError] = useState<string | null>(null);
  const [cantidad, setCantidad] = useState("");
  const [motivo, setMotivo] = useState("");

  useEffect(() => {
    if (!itemId) {
      setLotes([]);
      setLoteId("");
      setLotesError(null);
      return;
    }
    let cancelled = false;
    setLotesLoading(true);
    setLotesError(null);
    setLoteId("");
    apiGet<Lote[]>(`/api/stock/items/${itemId}/lotes`)
      .then((data) => {
        if (!cancelled) setLotes(data);
      })
      .catch((err) => {
        if (!cancelled) setLotesError(err instanceof Error ? err.message : "Error inesperado");
      })
      .finally(() => {
        if (!cancelled) setLotesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [itemId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!itemId) {
      setError("Seleccioná un item.");
      return;
    }
    if (!loteId) {
      setError("Seleccioná un lote.");
      return;
    }
    const cantidadNum = Number(cantidad);
    if (!cantidad || Number.isNaN(cantidadNum) || cantidadNum <= 0) {
      setError("Ingresá una cantidad mayor a cero.");
      return;
    }
    if (!motivo.trim()) {
      setError("El motivo es obligatorio.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/stock/mermas", {
        itemId: Number(itemId),
        loteId: Number(loteId),
        cantidad: cantidadNum,
        motivo: motivo.trim(),
      });
      await onCreated("Merma registrada correctamente.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Registrar merma" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="merma-item" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Item
          </label>
          <select
            id="merma-item"
            value={itemId}
            onChange={(e) => setItemId(e.target.value)}
            className={INPUT_CLASS}
          >
            <option value="">Seleccionar...</option>
            {items.map((i) => (
              <option key={i.id} value={i.id}>
                {i.sku} — {i.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="merma-lote" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Lote
          </label>
          {lotesLoading ? (
            <p className="text-sm text-neutral-500">Cargando lotes...</p>
          ) : lotesError ? (
            <p className="text-sm text-red-600 dark:text-red-400">{lotesError}</p>
          ) : (
            <select
              id="merma-lote"
              value={loteId}
              onChange={(e) => setLoteId(e.target.value)}
              className={INPUT_CLASS}
              disabled={!itemId || lotes.length === 0}
            >
              <option value="">Seleccionar...</option>
              {lotes.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.codigoLote} · ingreso {formatDate(l.fechaIngreso)} · {formatNumber(l.cantidadIngresada)}
                </option>
              ))}
            </select>
          )}
          {itemId && !lotesLoading && !lotesError && lotes.length === 0 && (
            <p className="text-sm text-neutral-500">Este item no tiene lotes registrados.</p>
          )}
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="merma-cantidad" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Cantidad
          </label>
          <input
            id="merma-cantidad"
            type="number"
            min="0.001"
            step="0.001"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Ej.: 2"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="merma-motivo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Motivo
          </label>
          <input
            id="merma-motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Ej.: Producto vencido"
          />
        </div>

        {error && <ErrorBox message={error} />}

        <ModalActions
          submitting={submitting}
          submitLabel={submitting ? "Registrando..." : "Registrar merma"}
          onClose={onClose}
        />
      </form>
    </Modal>
  );
}

function AjusteForm({
  items,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  items: Item[];
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: (message: string) => Promise<void>;
}) {
  const [itemId, setItemId] = useState("");
  const [cantidad, setCantidad] = useState("");
  const [motivo, setMotivo] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!itemId) {
      setError("Seleccioná un item.");
      return;
    }
    const cantidadNum = Number(cantidad);
    if (cantidad === "" || Number.isNaN(cantidadNum) || cantidadNum === 0) {
      setError("Ingresá una cantidad distinta de cero.");
      return;
    }
    if (!motivo.trim()) {
      setError("El motivo es obligatorio.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/stock/ajustes", {
        itemId: Number(itemId),
        cantidad: cantidadNum,
        motivo: motivo.trim(),
      });
      await onCreated("Ajuste de inventario registrado correctamente.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Ajuste de inventario" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ajuste-item" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Item
          </label>
          <select
            id="ajuste-item"
            value={itemId}
            onChange={(e) => setItemId(e.target.value)}
            className={INPUT_CLASS}
          >
            <option value="">Seleccionar...</option>
            {items.map((i) => (
              <option key={i.id} value={i.id}>
                {i.sku} — {i.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ajuste-cantidad" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Cantidad
          </label>
          <input
            id="ajuste-cantidad"
            type="number"
            step="0.001"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Negativo resta, positivo suma"
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <label htmlFor="ajuste-motivo" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Motivo
          </label>
          <input
            id="ajuste-motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            className={INPUT_CLASS}
            placeholder="Ej.: Diferencia en conteo físico"
          />
        </div>

        {error && <ErrorBox message={error} />}

        <ModalActions
          submitting={submitting}
          submitLabel={submitting ? "Guardando..." : "Registrar ajuste"}
          onClose={onClose}
        />
      </form>
    </Modal>
  );
}

function ModalActions({
  submitting,
  submitLabel,
  onClose,
}: {
  submitting: boolean;
  submitLabel: string;
  onClose: () => void;
}) {
  return (
    <div className="flex justify-end gap-2">
      <Button type="button" variant="secondary" onClick={onClose}>
        Cancelar
      </Button>
      <Button type="submit" disabled={submitting}>
        {submitLabel}
      </Button>
    </div>
  );
}
