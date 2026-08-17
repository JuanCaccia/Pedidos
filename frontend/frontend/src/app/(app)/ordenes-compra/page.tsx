"use client";

import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { apiGet, apiPost, apiUploadCsv } from "@/lib/api";
import type { Categoria, EstadoOrdenCompra, ImportacionCsvResponse, Item, OrdenCompra, PageResponse, Proveedor, StockInfo } from "@/lib/types";
import { formatDate, formatMoney, formatNumber } from "@/lib/format";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Drawer from "@/components/Drawer";
import Modal from "@/components/Modal";
import ConfirmacionFrictionada from "@/components/ConfirmacionFrictionada";
import Combobox from "@/components/Combobox";
import { IconClose } from "@/components/icons";

const ESTADOS: EstadoOrdenCompra[] = ["PENDIENTE", "RECIBIDA_PARCIAL", "RECIBIDA", "CANCELADA"];

const ESTADO_ORDEN_STYLES: Record<EstadoOrdenCompra, { badge: string; label: string }> = {
  PENDIENTE: {
    badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
    label: "Pendiente",
  },
  RECIBIDA_PARCIAL: {
    badge: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300",
    label: "Recibida parcial",
  },
  RECIBIDA: {
    badge: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300",
    label: "Recibida",
  },
  CANCELADA: {
    badge: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
    label: "Cancelada",
  },
};

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

interface FormLineaRow {
  key: number;
  itemId: number | null;
  itemLabel: string | null;
  cantidad: string;
}

export default function OrdenesCompraPage() {
  const [ordenes, setOrdenes] = useState<OrdenCompra[]>([]);
  const [proveedores, setProveedores] = useState<Proveedor[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [estado, setEstado] = useState<string>("");

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [recepcionId, setRecepcionId] = useState<number | null>(null);
  const [csvRecepcionId, setCsvRecepcionId] = useState<number | null>(null);

  const [showForm, setShowForm] = useState(false);
  const [cancelarOrden, setCancelarOrden] = useState<OrdenCompra | null>(null);

  const proveedoresById = useMemo(() => new Map(proveedores.map((p) => [p.id, p])), [proveedores]);
  const itemsById = useMemo(() => new Map(items.map((i) => [i.id, i])), [items]);

  const loadOrdenes = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (estado) params.set("estado", estado);
      const data = await apiGet<OrdenCompra[]>(`/api/ordenes-compra?${params.toString()}`);
      setOrdenes(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [estado]);

  useEffect(() => {
    loadOrdenes();
  }, [loadOrdenes]);

  useEffect(() => {
    Promise.all([
      apiGet<PageResponse<Proveedor>>("/api/proveedores?size=500"),
      apiGet<PageResponse<Item>>("/api/items?size=500"),
    ])
      .then(([prov, itemsData]) => {
        setProveedores(prov.content);
        setItems(itemsData.content);
      })
      .catch(() => {
        // El formulario de alta mostrará el error al intentar usarlo
      });
  }, []);

  async function cancelar(orden: OrdenCompra) {
    await apiPost<void>(`/api/ordenes-compra/${orden.id}/cancelar`);
    await loadOrdenes();
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Órdenes de compra</h1>
        <Button
          onClick={() => {
            setError(null);
            setShowForm(true);
          }}
        >
          Nueva orden
        </Button>
      </div>

      <div className="flex items-center gap-3">
        <label htmlFor="estado" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
          Estado
        </label>
        <select
          id="estado"
          value={estado}
          onChange={(e) => setEstado(e.target.value)}
          className={INPUT_CLASS}
        >
          <option value="">Todos</option>
          {ESTADOS.map((e) => (
            <option key={e} value={e}>
              {ESTADO_ORDEN_STYLES[e].label}
            </option>
          ))}
        </select>
      </div>

      {error && <ErrorBox message={error} />}

      {loading ? (
        <Loading />
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">Número</th>
                  <th className="px-4 py-3 font-medium">Proveedor</th>
                  <th className="px-4 py-3 font-medium">Fecha</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 text-right font-medium">Ítems</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {ordenes.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay órdenes de compra para mostrar
                    </td>
                  </tr>
                )}
                {ordenes.map((orden) => (
                  <OrdenRow
                    key={orden.id}
                    orden={orden}
                    proveedoresById={proveedoresById}
                    itemsById={itemsById}
                    expanded={expandedId === orden.id}
                    onToggle={() => setExpandedId(expandedId === orden.id ? null : orden.id)}
                    recepcionAbierta={recepcionId === orden.id}
                    onToggleRecepcion={() => setRecepcionId(recepcionId === orden.id ? null : orden.id)}
                    onImportarCsv={() => setCsvRecepcionId(orden.id)}
                    onCancelar={() => setCancelarOrden(orden)}
                    onRecepcionSuccess={loadOrdenes}
                  />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {cancelarOrden && (
        <ConfirmacionFrictionada
          title={`Cancelar orden ${cancelarOrden.numero}`}
          descripcion="La orden de compra quedará cancelada y no se podrán registrar recepciones sobre ella. No se puede deshacer."
          palabra="CANCELAR"
          confirmLabel="Cancelar orden"
          onConfirm={() => cancelar(cancelarOrden)}
          onClose={() => setCancelarOrden(null)}
        />
      )}

      {showForm && (
        <NuevaOrdenForm
          onClose={() => setShowForm(false)}
          onCreated={async () => {
            setShowForm(false);
            await loadOrdenes();
          }}
        />
      )}

      {csvRecepcionId != null && (
        <RecepcionCsvModal
          orden={ordenes.find((o) => o.id === csvRecepcionId) ?? null}
          onClose={() => setCsvRecepcionId(null)}
          onSuccess={async () => {
            setCsvRecepcionId(null);
            await loadOrdenes();
          }}
        />
      )}
    </div>
  );
}

function OrdenEstadoBadge({ estado }: { estado: EstadoOrdenCompra }) {
  const { badge, label } = ESTADO_ORDEN_STYLES[estado] ?? {
    badge: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300",
    label: estado,
  };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ${badge}`}>
      {label}
    </span>
  );
}

function OrdenRow({
  orden,
  proveedoresById,
  itemsById,
  expanded,
  onToggle,
  recepcionAbierta,
  onToggleRecepcion,
  onImportarCsv,
  onCancelar,
  onRecepcionSuccess,
}: {
  orden: OrdenCompra;
  proveedoresById: Map<number, Proveedor>;
  itemsById: Map<number, Item>;
  expanded: boolean;
  onToggle: () => void;
  recepcionAbierta: boolean;
  onToggleRecepcion: () => void;
  onImportarCsv: () => void;
  onCancelar: () => void;
  onRecepcionSuccess: () => Promise<void>;
}) {
  const proveedor = proveedoresById.get(orden.proveedorId);
  const puedeRecibir = orden.estado === "PENDIENTE" || orden.estado === "RECIBIDA_PARCIAL";

  return (
    <Fragment>
      <tr className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
        <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">{orden.numero}</td>
        <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
          {proveedor?.razonSocial ?? `#${orden.proveedorId}`}
        </td>
        <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{formatDate(orden.fecha)}</td>
        <td className="px-4 py-3">
          <OrdenEstadoBadge estado={orden.estado} />
        </td>
        <td className="px-4 py-3 text-right text-neutral-700 dark:text-neutral-300">
          {orden.lineas.length}
        </td>
        <td className="px-4 py-3">
          <div className="flex items-center justify-end gap-2">
            <Button variant="secondary" onClick={onToggle}>
              {expanded ? "Ocultar" : "Detalle"}
            </Button>
            {puedeRecibir && (
              <Button variant="secondary" onClick={onToggleRecepcion}>
                {recepcionAbierta ? "Cerrar recepción" : "Registrar recepción"}
              </Button>
            )}
            {puedeRecibir && (
              <Button variant="secondary" onClick={onImportarCsv}>
                Importar recepción (CSV)
              </Button>
            )}
            {puedeRecibir && (
              <Button
                variant="secondary"
                onClick={onCancelar}
                className="text-red-600 dark:text-red-400"
              >
                Cancelar
              </Button>
            )}
          </div>
        </td>
      </tr>
      {expanded && (
        <tr className="bg-neutral-50 dark:bg-neutral-800/30">
          <td colSpan={6} className="px-4 py-4">
            <div className="flex flex-col gap-4">
              <div>
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-500">Items</h3>
                <div className="overflow-hidden rounded-md border border-neutral-200 dark:border-neutral-800">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-neutral-200 bg-white text-xs uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900">
                        <th className="px-3 py-2 text-left font-medium">Item</th>
                        <th className="px-3 py-2 text-right font-medium">Pedida</th>
                        <th className="px-3 py-2 text-right font-medium">Recibida</th>
                        <th className="px-3 py-2 text-right font-medium">Restante</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                      {orden.lineas.map((linea) => {
                        const item = itemsById.get(linea.itemId);
                        return (
                          <tr key={linea.id}>
                            <td className="px-3 py-2 text-neutral-700 dark:text-neutral-300">
                              {item ? `${item.sku} — ${item.nombre}` : `#${linea.itemId}`}
                            </td>
                            <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                              {formatNumber(linea.cantidadPedida)}
                            </td>
                            <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                              {formatNumber(linea.cantidadRecibida)}
                            </td>
                            <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                              {formatNumber(linea.restante)}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
              {orden.observaciones && (
                <p className="text-sm text-neutral-600 dark:text-neutral-400">
                  Observaciones: {orden.observaciones}
                </p>
              )}
            </div>
          </td>
        </tr>
      )}
      {recepcionAbierta && (
        <tr className="bg-blue-50/40 dark:bg-blue-950/10">
          <td colSpan={6} className="px-4 py-4">
            <RecepcionPanel orden={orden} itemsById={itemsById} onSuccess={onRecepcionSuccess} />
          </td>
        </tr>
      )}
    </Fragment>
  );
}

function RecepcionPanel({
  orden,
  itemsById,
  onSuccess,
}: {
  orden: OrdenCompra;
  itemsById: Map<number, Item>;
  onSuccess: () => Promise<void>;
}) {
  const [values, setValues] = useState<Record<number, string>>(() =>
    Object.fromEntries(orden.lineas.map((l) => [l.id, String(l.restante)]))
  );
  const [precios, setPrecios] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit() {
    if (saving) return;
    setError(null);

    const lineas = orden.lineas
      .map((l) => {
        const parsed = Number(values[l.id] ?? "");
        const cantidadRecibida = Number.isNaN(parsed) ? 0 : Math.min(Math.max(parsed, 0), l.restante);
        const precio = Number(precios[l.id] ?? "");
        return { lineaId: l.id, cantidadRecibida, precioUnitario: precio };
      })
      .filter((l) => l.cantidadRecibida > 0);

    if (lineas.length === 0) {
      setError("Ingresá al menos una cantidad mayor a cero.");
      return;
    }
    if (lineas.some((l) => Number.isNaN(l.precioUnitario) || l.precioUnitario <= 0)) {
      setError("Ingresá un precio unitario mayor a cero para cada línea recibida.");
      return;
    }

    setSaving(true);
    try {
      await apiPost(`/api/ordenes-compra/${orden.id}/recepciones`, { lineas });
      await onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
        Registrar recepción · {orden.numero}
      </h3>
      <div className="space-y-2">
        {orden.lineas.map((linea) => {
          const item = itemsById.get(linea.itemId);
          const precioIngresado = Number(precios[linea.id] ?? "");
          const precioSuperaLista =
            !Number.isNaN(precioIngresado) && item != null && precioIngresado > item.precioLista;
          return (
            <div key={linea.id} className="flex flex-col gap-1">
              <div className="grid grid-cols-[1fr_6rem_7rem_auto] items-center gap-2">
                <span className="truncate text-sm text-neutral-700 dark:text-neutral-300">
                  {item ? `${item.sku} — ${item.nombre}` : `#${linea.itemId}`}
                </span>
                <input
                  type="number"
                  min="0.001"
                  step="0.001"
                  max={linea.restante}
                  value={values[linea.id] ?? ""}
                  onChange={(e) =>
                    setValues((prev) => ({ ...prev, [linea.id]: e.target.value }))
                  }
                  aria-label={`Cantidad a recibir línea ${linea.id}`}
                  placeholder="Cant."
                  className={INPUT_CLASS}
                />
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={precios[linea.id] ?? ""}
                  onChange={(e) =>
                    setPrecios((prev) => ({ ...prev, [linea.id]: e.target.value }))
                  }
                  aria-label={`Precio unitario línea ${linea.id}`}
                  placeholder="Precio"
                  className={INPUT_CLASS}
                />
                <span className="text-xs text-neutral-500">restante: {formatNumber(linea.restante)}</span>
              </div>
              {precioSuperaLista && (
                <p className="text-xs font-medium text-amber-700 dark:text-amber-400">
                  El costo supera el precio de venta ({formatMoney(item?.precioLista ?? 0)}). Verificá el costo
                  ingresado.
                </p>
              )}
            </div>
          );
        })}
      </div>
      {error && <ErrorBox message={error} />}
      <div className="flex justify-end gap-2">
        <Button onClick={handleSubmit} disabled={saving}>
          {saving ? "Registrando..." : "Registrar recepción"}
        </Button>
      </div>
    </div>
  );
}

function RecepcionCsvModal({
  orden,
  onClose,
  onSuccess,
}: {
  orden: OrdenCompra | null;
  onClose: () => void;
  onSuccess: () => Promise<void>;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportacionCsvResponse | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit() {
    if (saving) return;
    if (!file) {
      setError("Seleccioná un archivo CSV.");
      return;
    }
    if (!orden) return;
    setError(null);
    setResult(null);
    setSaving(true);
    try {
      const res = await apiUploadCsv<ImportacionCsvResponse>(
        `/api/ordenes-compra/${orden.id}/recepciones/csv`,
        file
      );
      setResult(res);
      if (res.errores.length === 0) {
        await onSuccess();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={orden ? `Importar recepción (CSV) · ${orden.numero}` : "Importar recepción (CSV)"} onClose={onClose}>
      <div className="flex flex-col gap-4">
        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          Cada fila del CSV debe tener: <code className="font-mono">sku, cantidad, precioUnitario, fechaVencimiento, codigoLote</code>.
          El separador puede ser <code className="font-mono">;</code> o <code className="font-mono">,</code>. Solo se reciben
          los sku que figuran en la orden; las líneas que no aparecen quedan pendientes.
        </p>
        <input
          type="file"
          accept=".csv,text/csv"
          onChange={(e) => {
            setFile(e.target.files?.[0] ?? null);
            setResult(null);
            setError(null);
          }}
          className={INPUT_CLASS}
        />
        {error && <ErrorBox message={error} />}
        {result && result.errores.length === 0 && (
          <div className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-300">
            Se importaron {result.lotesCreados.length} lote{result.lotesCreados.length === 1 ? "" : "s"} correctamente.
          </div>
        )}
        {result && result.errores.length > 0 && (
          <div className="rounded-md border border-red-200 bg-red-50 p-3 dark:border-red-900 dark:bg-red-950/40">
            <p className="mb-1 text-sm font-medium text-red-800 dark:text-red-300">Errores por fila:</p>
            <ul className="list-inside list-disc space-y-0.5 text-sm text-red-700 dark:text-red-400">
              {result.errores.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
          </div>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cerrar
          </Button>
          <Button onClick={handleSubmit} disabled={saving}>
            {saving ? "Importando..." : "Importar CSV"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function NuevaOrdenForm({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [proveedorId, setProveedorId] = useState<number | null>(null);
  const [observaciones, setObservaciones] = useState("");
  const [categoriaId, setCategoriaId] = useState("");
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [disponiblesById, setDisponiblesById] = useState<Map<number, number>>(new Map());
  const [lineas, setLineas] = useState<FormLineaRow[]>([
    { key: Date.now(), itemId: null, itemLabel: null, cantidad: "" },
  ]);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    apiGet<Categoria[]>("/api/categorias")
      .then(setCategorias)
      .catch(() => {
        // El select de categorías mostrará solo "Todas las categorías"
      });
  }, []);

  useEffect(() => {
    const params = new URLSearchParams({ size: "500" });
    if (categoriaId) params.set("categoriaId", categoriaId);
    params.set("activos", "true");
    apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`)
      .then((data) => setItems(data.content))
      .catch(() => {
        setItems([]);
      });
  }, [categoriaId]);

  function addLinea() {
    setLineas((prev) => [...prev, { key: Date.now() + Math.random(), itemId: null, itemLabel: null, cantidad: "" }]);
  }

  function updateLinea<K extends keyof FormLineaRow>(key: number, field: K, value: FormLineaRow[K]) {
    setLineas((prev) => prev.map((l) => (l.key === key ? { ...l, [field]: value } : l)));
  }

  function removeLinea(key: number) {
    setLineas((prev) => prev.filter((l) => l.key !== key));
  }

  /**
   * Disponibilidad por ítem vía GET /api/stock/items/{id} (endpoint `authenticated`),
   * en paralelo y cacheada en `disponiblesById`. Se usa el mismo mecanismo que el
   * form de pedidos para no depender de /api/reportes/stock: aunque OC solo lo
   * abren ENCARGADO_DEPOSITO/ADMINISTRATIVO (SecurityConfig los habilita en
   * /ordenes-compra/**), este endpoint es `authenticated` y evita una dependencia
   * frágil si algún rol con 403 llegara a abrir el form.
   */
  async function obtenerDisponibles(ids: number[]): Promise<Map<number, number>> {
    const map = new Map(disponiblesById);
    const missing = ids.filter((id) => !map.has(id));
    if (missing.length > 0) {
      const results = await Promise.allSettled(
        missing.map((id) => apiGet<StockInfo>(`/api/stock/items/${id}`))
      );
      results.forEach((r, idx) => {
        const id = missing[idx];
        if (r.status === "fulfilled") map.set(id, r.value.disponible);
      });
      setDisponiblesById(new Map(map));
    }
    return map;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (saving) return;
    setError(null);

    if (proveedorId == null) {
      setError("Seleccioná un proveedor.");
      return;
    }
    const lineasValidas = lineas.filter((l) => l.itemId != null && Number(l.cantidad) > 0);
    if (lineasValidas.length === 0) {
      setError("Agregá al menos una línea con un item y cantidad mayor a cero.");
      return;
    }

    setSaving(true);
    try {
      await apiPost("/api/ordenes-compra", {
        proveedorId,
        observaciones: observaciones.trim() || undefined,
        lineas: lineasValidas.map((l) => ({
          itemId: l.itemId as number,
          cantidad: Number(l.cantidad),
        })),
      });
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Drawer title="Nueva orden de compra" onClose={onClose} width="lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Combobox
          label="Proveedor"
          placeholder="Buscar proveedor..."
          required
          value={proveedorId}
          onChange={setProveedorId}
          search={async (q) => {
            const data = await apiGet<PageResponse<Proveedor>>(
              `/api/proveedores?q=${encodeURIComponent(q)}&size=20`
            );
            return data.content.map((p) => ({
              id: p.id,
              label: p.razonSocial,
              sublabel: p.cuit,
            }));
          }}
        />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="oc-observaciones" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Observaciones
          </label>
          <textarea
            id="oc-observaciones"
            value={observaciones}
            onChange={(e) => setObservaciones(e.target.value)}
            rows={2}
            className={INPUT_CLASS}
          />
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Líneas</span>
            <button
              type="button"
              onClick={addLinea}
              className="rounded-md border border-neutral-300 px-2.5 py-1 text-xs font-medium text-neutral-700 transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              + Agregar línea
            </button>
          </div>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <label
              htmlFor="oc-categoria"
              className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
            >
              Categoría
            </label>
            <select
              id="oc-categoria"
              value={categoriaId}
              onChange={(e) => {
                setCategoriaId(e.target.value);
              }}
              className={INPUT_CLASS}
            >
              <option value="">Todas las categorías</option>
              {categorias.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            {lineas.map((linea) => (
              <div key={linea.key} className="grid grid-cols-[1fr_5rem_auto] gap-2">
                <Combobox
                  key={linea.key}
                  placeholder="Buscar item..."
                  value={linea.itemId}
                  valueLabel={linea.itemLabel}
                  onChange={(id) => {
                    updateLinea(linea.key, "itemId", id);
                    if (id != null) {
                      const item = items.find((i) => i.id === id);
                      updateLinea(linea.key, "itemLabel", item ? `${item.sku} — ${item.nombre}` : null);
                    } else {
                      updateLinea(linea.key, "itemLabel", null);
                    }
                  }}
                  search={async (q) => {
                    const params = new URLSearchParams({ q, size: "20" });
                    if (categoriaId) params.set("categoriaId", categoriaId);
                    params.set("activos", "true");
                    const data = await apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`);
                    const disponibles = await obtenerDisponibles(data.content.map((i) => i.id));
                    return data.content.map((i) => {
                      const disponible = disponibles.get(i.id);
                      return {
                        id: i.id,
                        label: `${i.sku} — ${i.nombre}`,
                        sublabel: [
                          i.categoriaNombre ?? "",
                          disponible != null ? `Disponible: ${formatNumber(disponible)} un.` : "",
                        ]
                          .filter(Boolean)
                          .join(" · "),
                      };
                    });
                  }}
                />
                <input
                  type="number"
                  min="0"
                  step="0.001"
                  placeholder="Cant."
                  value={linea.cantidad}
                  onChange={(e) => updateLinea(linea.key, "cantidad", e.target.value)}
                  className={INPUT_CLASS}
                />
                <button
                  type="button"
                  onClick={() => removeLinea(linea.key)}
                  disabled={lineas.length === 1}
                  aria-label="Quitar línea"
                  className="rounded-md border border-neutral-200 px-2 text-sm text-neutral-400 transition-colors hover:bg-neutral-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-neutral-700 dark:hover:bg-neutral-800"
                >
                  <IconClose />
                </button>
              </div>
            ))}
          </div>
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Creando..." : "Crear orden"}
          </Button>
        </div>
      </form>
    </Drawer>
  );
}
