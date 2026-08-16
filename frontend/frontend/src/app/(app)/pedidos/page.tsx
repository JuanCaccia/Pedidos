"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import type { ApiError, Cliente, Item, Pedido, PedidoItem, EstadoPedido, PageResponse, Sustitucion } from "@/lib/types";
import { formatDateTime, formatMoney, formatNumber } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import EstadoBadge from "@/components/EstadoBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Pagination from "@/components/Pagination";
import Button from "@/components/Button";
import Drawer from "@/components/Drawer";
import Modal from "@/components/Modal";
import ConfirmacionFrictionada from "@/components/ConfirmacionFrictionada";
import Combobox from "@/components/Combobox";
import { IconClose } from "@/components/icons";
import { exportarCSV } from "@/lib/export";

const PAGE_SIZE = 20;

type ContadoresPedidos = Record<EstadoPedido, number>;

interface TabDef {
  estado: EstadoPedido | "";
  label: string;
  title: string;
  badgeClass: string;
}

const TABS: TabDef[] = [
  { estado: "", label: "Todos", title: "Todos", badgeClass: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300" },
  { estado: "PENDIENTE_CONFIRMACION", label: "Conf.", title: "Pendiente confirmación", badgeClass: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300" },
  { estado: "PENDIENTE_STOCK", label: "Sin stock", title: "Pendiente de stock", badgeClass: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300" },
  { estado: "PENDIENTE_PREPARACION", label: "Prep.", title: "Pendiente preparación", badgeClass: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300" },
  { estado: "PENDIENTE_ENTREGA", label: "Desp.", title: "Pendiente entrega", badgeClass: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300" },
  { estado: "EN_VIAJE", label: "En viaje", title: "En viaje", badgeClass: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300" },
  { estado: "ENTREGADO", label: "Entregado", title: "Entregado", badgeClass: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300" },
  { estado: "ENTREGADO_PARCIAL", label: "Parcial", title: "Entregado parcial", badgeClass: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300" },
  { estado: "RE_AGENDADO", label: "Re-agend.", title: "Re-agendado", badgeClass: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300" },
  { estado: "RECHAZADO", label: "Rechaz.", title: "Rechazado", badgeClass: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300" },
];

interface FormItemRow {
  key: number;
  itemId: number | null;
  itemLabel: string | null;
  cantidad: string;
  precioUnitario: string;
}

export default function PedidosPage() {
  const { user } = useAuth();
  const hasAnyRole = (...roles: string[]) => roles.some((r) => user?.roles.includes(r) ?? false);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [estado, setEstado] = useState<string>("");
  const [soloPendienteStock, setSoloPendienteStock] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [contadores, setContadores] = useState<ContadoresPedidos | null>(null);
  const [enViajeClienteIds, setEnViajeClienteIds] = useState<Set<number>>(new Set());

  const [clientes, setClientes] = useState<Cliente[]>([]);

  const clientesById = useMemo(() => new Map(clientes.map((c) => [c.id, c])), [clientes]);

  const [detalleId, setDetalleId] = useState<number | null>(null);
  const [verHijosActivo, setVerHijosActivo] = useState(false);
  const [hijos, setHijos] = useState<Record<number, Pedido[]>>({});
  const [hijosLoading, setHijosLoading] = useState<Record<number, boolean>>({});
  const [hijosError, setHijosError] = useState<Record<number, string>>({});

  const [seleccionados, setSeleccionados] = useState<Set<number>>(new Set());
  const [despachando, setDespachando] = useState(false);
  const [batchMessage, setBatchMessage] = useState<string | null>(null);
  const [consolidando, setConsolidando] = useState(false);
  const [consolidarMessage, setConsolidarMessage] = useState<string | null>(null);

  const [rechazarPedido, setRechazarPedido] = useState<Pedido | null>(null);

  const [sustituirPedido, setSustituirPedido] = useState<Pedido | null>(null);
  const [sustitucionMessage, setSustitucionMessage] = useState<string | null>(null);

  const [marcarFaltante, setMarcarFaltante] = useState<{ pedido: Pedido; item: PedidoItem } | null>(null);

  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const esPreparacion = estado === "PENDIENTE_PREPARACION";
  const esConfirmacion = estado === "PENDIENTE_CONFIRMACION";
  const puedeConsolidar = esConfirmacion && hasAnyRole("VENDEDOR", "ADMINISTRATIVO");
  const mostrarCheckboxes = esPreparacion || puedeConsolidar;

  const loadContadores = useCallback(async () => {
    try {
      const data = await apiGet<ContadoresPedidos>("/api/pedidos/contadores");
      setContadores(data);
    } catch {
      // Los contadores son complementarios; los errores del listado ya se muestran
    }
  }, []);

  const loadEnViajeClientes = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Pedido>>("/api/pedidos?estado=EN_VIAJE&size=500");
      setEnViajeClienteIds(new Set(data.content.map((p) => p.clienteId)));
    } catch {
      setEnViajeClienteIds(new Set());
    }
  }, []);

  const loadPedidos = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (estado) params.set("estado", estado);
      const data = await apiGet<PageResponse<Pedido>>(`/api/pedidos?${params.toString()}`);
      setPedidos(data.content);
      setTotalPages(data.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
    loadContadores();
    loadEnViajeClientes();
  }, [estado, page, loadContadores, loadEnViajeClientes]);

  useEffect(() => {
    loadPedidos();
  }, [loadPedidos]);

  useEffect(() => {
    apiGet<PageResponse<Cliente>>("/api/clientes?size=500")
      .then((clientesData) => {
        setClientes(clientesData.content);
      })
      .catch(() => {
        // El formulario de alta mostrará el error al intentar usarlo
      });
  }, []);

  async function confirmar(pedido: Pedido) {
    try {
      await apiPost<Pedido>(`/api/pedidos/${pedido.id}/confirmar`);
      await loadPedidos();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function despachar(pedido: Pedido) {
    try {
      await apiPost<Pedido>(`/api/pedidos/${pedido.id}/despachar`);
      await loadPedidos();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function reagendar(pedido: Pedido) {
    try {
      await apiPost<Pedido>(`/api/pedidos/${pedido.id}/reagendar`);
      await loadPedidos();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function rechazar(pedido: Pedido) {
    await apiPost<Pedido>(`/api/pedidos/${pedido.id}/rechazar`);
    await loadPedidos();
  }

  async function despacharSeleccionados() {
    if (despachando) return;
    const ids = pedidosVisibles.filter((p) => seleccionados.has(p.id)).map((p) => p.id);
    if (ids.length === 0) return;
    setDespachando(true);
    setBatchMessage(null);
    setError(null);
    try {
      const results = await Promise.allSettled(
        ids.map((id) => apiPost<Pedido>(`/api/pedidos/${id}/despachar`))
      );
      const exitosos = results.filter((r) => r.status === "fulfilled").length;
      const fallidos = ids.length - exitosos;
      if (fallidos === 0) {
        setBatchMessage(
          `${exitosos} ${exitosos === 1 ? "pedido despachado" : "pedidos despachados"}`
        );
      } else {
        setError(`Se despacharon ${exitosos} de ${ids.length} pedidos. Revisá los errores del resto.`);
      }
      setSeleccionados(new Set());
      await loadPedidos();
    } finally {
      setDespachando(false);
    }
  }

  function consolidarErrorMsg(err: unknown): string {
    const e = err as Error & { apiError?: ApiError };
    const code = e?.apiError?.code;
    if (code === "CONSOLIDAR_CLIENTES_DISTINTOS") {
      return "Los pedidos deben pertenecer al mismo cliente.";
    }
    if (code === "CONSOLIDAR_PRECIOS_DISTINTOS") {
      return "Los pedidos tienen precios distintos para el mismo item.";
    }
    if (code === "PEDIDO_ESTADO_INVALIDO") {
      return "Uno de los pedidos no está pendiente de confirmación.";
    }
    return e instanceof Error ? e.message : "Error inesperado";
  }

  async function consolidarSeleccionados() {
    if (consolidando) return;
    const ids = pedidosVisibles.filter((p) => seleccionados.has(p.id)).map((p) => p.id);
    if (ids.length < 2) return;
    setConsolidando(true);
    setConsolidarMessage(null);
    setError(null);
    try {
      const nuevo = await apiPost<Pedido>("/api/pedidos/consolidar", { pedidoIds: ids });
      setConsolidarMessage(
        `Pedido ${nuevo.numero} creado consolidando ${ids.length} pedidos.`
      );
      setSeleccionados(new Set());
      await loadPedidos();
    } catch (err) {
      setError(consolidarErrorMsg(err));
    } finally {
      setConsolidando(false);
    }
  }

  async function verHijos(pedido: Pedido) {
    if (hijos[pedido.id] && !hijosLoading[pedido.id]) {
      setVerHijosActivo((v) => !v);
      return;
    }
    setHijosLoading((prev) => ({ ...prev, [pedido.id]: true }));
    setHijosError((prev) => ({ ...prev, [pedido.id]: "" }));
    try {
      const hijosPedidos = await apiGet<Pedido[]>(`/api/pedidos/${pedido.id}/hijos`);
      setHijos((prev) => ({ ...prev, [pedido.id]: hijosPedidos }));
      setVerHijosActivo(true);
    } catch (err) {
      setHijosError((prev) => ({
        ...prev,
        [pedido.id]: err instanceof Error ? err.message : "Error inesperado",
      }));
    } finally {
      setHijosLoading((prev) => ({ ...prev, [pedido.id]: false }));
    }
  }

  function openDetalle(pedido: Pedido) {
    setDetalleId(pedido.id);
    setVerHijosActivo(false);
  }

  function cambiarTab(nuevoEstado: string) {
    setPage(0);
    setEstado(nuevoEstado);
    setSeleccionados(new Set());
    setBatchMessage(null);
    setConsolidarMessage(null);
  }

  function toggleSoloPendienteStock(checked: boolean) {
    setSoloPendienteStock(checked);
    setSeleccionados(new Set());
    setBatchMessage(null);
    setConsolidarMessage(null);
  }

  async function exportar() {
    try {
      await exportarCSV(
        "/api/pedidos/exportar.csv" + (estado ? "?estado=" + estado : ""),
        "pedidos.csv"
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  const pedidosVisibles = soloPendienteStock
    ? pedidos.filter((p) => p.estado === "PENDIENTE_STOCK" || p.items.some((i) => i.pendienteStock))
    : pedidos;

  const detallePedido =
    detalleId != null ? pedidos.find((p) => p.id === detalleId) ?? null : null;

  const todosVisiblesSeleccionados =
    pedidosVisibles.length > 0 && pedidosVisibles.every((p) => seleccionados.has(p.id));

  const totalContadores = useMemo(
    () => (contadores ? Object.values(contadores).reduce((acc, n) => acc + n, 0) : 0),
    [contadores]
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Pedidos</h1>
        <Button
          onClick={() => {
            setFormError(null);
            setShowForm(true);
          }}
        >
          Nuevo pedido
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex gap-2 overflow-x-auto pb-1">
          {TABS.map((tab) => {
            const active = estado === tab.estado;
            const count = tab.estado === "" ? totalContadores : (contadores?.[tab.estado] ?? 0);
            return (
              <button
                key={tab.estado || "todos"}
                type="button"
                aria-pressed={active}
                title={tab.title}
                onClick={() => cambiarTab(tab.estado)}
                className={`inline-flex shrink-0 items-center gap-1.5 rounded-md border px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors ${
                  active
                    ? "border-blue-600 bg-blue-50 text-blue-700 ring-2 ring-blue-600/40 dark:border-blue-500 dark:bg-blue-950/40 dark:text-blue-200 dark:ring-blue-500/40"
                    : "border-neutral-300 bg-white text-neutral-700 hover:bg-neutral-100 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-300 dark:hover:bg-neutral-700"
                }`}
              >
                {tab.label}
                <span className={`rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums ${tab.badgeClass}`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>
        <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-neutral-700 dark:text-neutral-300">
          <input
            type="checkbox"
            checked={soloPendienteStock}
            onChange={(e) => toggleSoloPendienteStock(e.target.checked)}
            className="h-4 w-4 rounded border-neutral-300 text-blue-600 focus:ring-blue-500 dark:border-neutral-700 dark:bg-neutral-800"
          />
          Solo con stock pendiente
        </label>
        <Button variant="secondary" className="px-3 py-1.5" onClick={exportar}>
          Exportar CSV
        </Button>
      </div>

      {esPreparacion && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Button
            onClick={despacharSeleccionados}
            disabled={seleccionados.size === 0 || despachando}
          >
            {despachando
              ? "Despachando..."
              : `Despachar seleccionados (${seleccionados.size})`}
          </Button>
          {batchMessage && (
            <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
              {batchMessage}
            </span>
          )}
        </div>
      )}

      {puedeConsolidar && seleccionados.size >= 2 && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Button onClick={consolidarSeleccionados} disabled={consolidando}>
            {consolidando
              ? "Consolidando..."
              : `Consolidar seleccionados (${seleccionados.size})`}
          </Button>
          {consolidarMessage && (
            <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
              {consolidarMessage}
            </span>
          )}
        </div>
      )}

      {error && <ErrorBox message={error} />}

      {sustitucionMessage && (
        <div
          role="status"
          className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300"
        >
          {sustitucionMessage}
        </div>
      )}

      {loading ? (
        <Loading />
      ) : (
        <div className="overflow-hidden rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  {mostrarCheckboxes && (
                    <th className="px-4 py-3">
                      <input
                        type="checkbox"
                        aria-label="Seleccionar todos los pedidos visibles"
                        checked={todosVisiblesSeleccionados}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSeleccionados(new Set(pedidosVisibles.map((p) => p.id)));
                          } else {
                            setSeleccionados(new Set());
                          }
                        }}
                        className="h-4 w-4 rounded border-neutral-300 text-blue-600 focus:ring-blue-500 dark:border-neutral-700 dark:bg-neutral-800"
                      />
                    </th>
                  )}
                  <th className="px-4 py-3 font-medium">Número</th>
                  <th className="px-4 py-3 font-medium">Cliente</th>
                  <th className="px-4 py-3 font-medium">Zona</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 font-medium text-right">Total</th>
                  <th className="px-4 py-3 font-medium">Creado</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {pedidosVisibles.length === 0 && (
                  <tr>
                    <td
                      colSpan={mostrarCheckboxes ? 8 : 7}
                      className="px-4 py-8 text-center text-neutral-500"
                    >
                      {soloPendienteStock
                        ? "No hay pedidos con stock pendiente"
                        : "No hay pedidos para mostrar"}
                    </td>
                  </tr>
                )}
                {pedidosVisibles.map((pedido) => (
                  <PedidoRow
                    key={pedido.id}
                    pedido={pedido}
                    clientesById={clientesById}
                    enRutaHoy={enViajeClienteIds.has(pedido.clienteId)}
                    onDetalle={() => openDetalle(pedido)}
                    mostrarCheckbox={mostrarCheckboxes}
                    seleccionado={seleccionados.has(pedido.id)}
                    onToggleSeleccion={(checked) => {
                      setSeleccionados((prev) => {
                        const next = new Set(prev);
                        if (checked) next.add(pedido.id);
                        else next.delete(pedido.id);
                        return next;
                      });
                    }}
                  />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && (
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      )}

      {detallePedido && (
        <Drawer title={`Pedido ${detallePedido.numero}`} onClose={() => setDetalleId(null)}>
          <PedidoDetalle
            pedido={detallePedido}
            clientesById={clientesById}
            hasAnyRole={hasAnyRole}
            onConfirm={() => confirmar(detallePedido)}
            onDespachar={() => despachar(detallePedido)}
            onReagendar={() => reagendar(detallePedido)}
            onRechazar={() => setRechazarPedido(detallePedido)}
            onSustituir={() => {
              setSustitucionMessage(null);
              setSustituirPedido(detallePedido);
            }}
            onMarcarFaltante={(item) => setMarcarFaltante({ pedido: detallePedido, item })}
            onVerHijos={() => verHijos(detallePedido)}
            verHijosActivo={verHijosActivo}
            hijos={hijos[detallePedido.id]}
            hijosLoading={!!hijosLoading[detallePedido.id]}
            hijosError={hijosError[detallePedido.id]}
          />
        </Drawer>
      )}

      {marcarFaltante && (
        <MarcarFaltanteModal
          pedido={marcarFaltante.pedido}
          item={marcarFaltante.item}
          onClose={() => setMarcarFaltante(null)}
          onConfirm={async () => {
            await loadPedidos();
          }}
        />
      )}

      {sustituirPedido && (
        <SustitucionModal
          pedido={sustituirPedido}
          onClose={() => setSustituirPedido(null)}
          onConfirm={async () => {
            setSustituirPedido(null);
            setSustitucionMessage(
              `Sustitución registrada en el pedido ${sustituirPedido.numero}.`
            );
            await loadPedidos();
          }}
        />
      )}

      {rechazarPedido && (
        <ConfirmacionFrictionada
          title={`Rechazar pedido ${rechazarPedido.numero}`}
          descripcion="Esta acción liberará la reserva de stock de los items del pedido y lo dejará en estado Rechazado. No se puede deshacer."
          palabra="RECHAZAR"
          confirmLabel="Rechazar pedido"
          onConfirm={() => rechazar(rechazarPedido)}
          onClose={() => setRechazarPedido(null)}
        />
      )}

      {showForm && (
        <NuevoPedidoForm
          vendedorId={user?.usuarioId ?? 0}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setShowForm(false)}
          onCreated={async () => {
            setShowForm(false);
            await loadPedidos();
          }}
        />
      )}
    </div>
  );
}

function PedidoRow({
  pedido,
  clientesById,
  enRutaHoy,
  onDetalle,
  mostrarCheckbox,
  seleccionado,
  onToggleSeleccion,
}: {
  pedido: Pedido;
  clientesById: Map<number, Cliente>;
  enRutaHoy: boolean;
  onDetalle: () => void;
  mostrarCheckbox: boolean;
  seleccionado: boolean;
  onToggleSeleccion: (checked: boolean) => void;
}) {
  const cliente = clientesById.get(pedido.clienteId);

  return (
    <tr className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
      {mostrarCheckbox && (
        <td className="px-4 py-3">
          <input
            type="checkbox"
            aria-label={`Seleccionar pedido ${pedido.numero}`}
            checked={seleccionado}
            onChange={(e) => onToggleSeleccion(e.target.checked)}
            className="h-4 w-4 rounded border-neutral-300 text-blue-600 focus:ring-blue-500 dark:border-neutral-700 dark:bg-neutral-800"
          />
        </td>
      )}
      <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">
        <span className="flex items-center gap-2">
          {pedido.numero}
          {pedido.express && (
            <span className="inline-flex items-center rounded-full bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
              Express
            </span>
          )}
        </span>
      </td>
      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
        {cliente?.razonSocial ?? `#${pedido.clienteId}`}
      </td>
      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cliente?.zonaNombre ?? "-"}</td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <EstadoBadge estado={pedido.estado} />
          {enRutaHoy && pedido.estado === "PENDIENTE_CONFIRMACION" && (
            <span className="inline-flex whitespace-nowrap rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
              en ruta hoy
            </span>
          )}
        </div>
      </td>
      <td className="px-4 py-3 text-right text-neutral-700 dark:text-neutral-300">{formatMoney(pedido.total)}</td>
      <td className="px-4 py-3 text-neutral-500">{formatDateTime(pedido.fechaCreacion)}</td>
      <td className="px-4 py-3 text-right">
        <Button variant="secondary" onClick={onDetalle}>
          Detalle
        </Button>
      </td>
    </tr>
  );
}

function PedidoDetalle({
  pedido,
  clientesById,
  hasAnyRole,
  onConfirm,
  onDespachar,
  onReagendar,
  onRechazar,
  onSustituir,
  onMarcarFaltante,
  onVerHijos,
  verHijosActivo,
  hijos,
  hijosLoading,
  hijosError,
}: {
  pedido: Pedido;
  clientesById: Map<number, Cliente>;
  hasAnyRole: (...roles: string[]) => boolean;
  onConfirm: () => void;
  onDespachar: () => void;
  onReagendar: () => void;
  onRechazar: () => void;
  onSustituir: () => void;
  onMarcarFaltante: (item: PedidoItem) => void;
  onVerHijos: () => void;
  verHijosActivo: boolean;
  hijos: Pedido[] | undefined;
  hijosLoading: boolean;
  hijosError: string | undefined;
}) {
  const cliente = clientesById.get(pedido.clienteId);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-2">
        <EstadoBadge estado={pedido.estado} />
        {cliente && (
          <span className="text-sm text-neutral-600 dark:text-neutral-400">
            {cliente.razonSocial}
            {cliente.zonaNombre ? ` · ${cliente.zonaNombre}` : ""}
          </span>
        )}
      </div>

      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-500">Items</h3>
        <div className="overflow-hidden rounded-md border border-neutral-200 dark:border-neutral-800">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-white text-xs uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900">
                <th className="px-3 py-2 font-medium text-left">Item</th>
                <th className="px-3 py-2 font-medium text-right">Pedida</th>
                <th className="px-3 py-2 font-medium text-right">Reservada</th>
                <th className="px-3 py-2 font-medium text-right">Entregada</th>
                <th className="px-3 py-2 font-medium text-right">Precio</th>
                {pedido.estado === "PENDIENTE_PREPARACION" &&
                  hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO") && (
                    <th className="px-3 py-2 font-medium text-right" />
                  )}
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
              {pedido.items.map((item: PedidoItem) => (
                <tr key={item.pedidoItemId}>
                  <td className="px-3 py-2 text-neutral-700 dark:text-neutral-300">
                    #{item.itemId}
                    {item.pendienteStock && (
                      <span className="ml-2 rounded-full bg-orange-100 px-2 py-0.5 text-xs text-orange-700 dark:bg-orange-900/40 dark:text-orange-300">
                        sin stock
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                    {formatNumber(item.cantidadPedida)}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                    {formatNumber(item.cantidadReservada)}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                    {formatNumber(item.cantidadEntregada)}
                  </td>
                  <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                    {formatMoney(item.precioUnitario)}
                  </td>
                  {pedido.estado === "PENDIENTE_PREPARACION" &&
                    hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO") && (
                      <td className="px-3 py-2 text-right">
                        <Button
                          variant="secondary"
                          className="px-2.5 py-1 text-xs"
                          onClick={() => onMarcarFaltante(item)}
                        >
                          Marcar faltante
                        </Button>
                      </td>
                    )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-500">Detalle</h3>
        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          Fecha jornada: {pedido.fechaJornada ?? "-"}
        </p>
        {pedido.pedidoPadreId && (
          <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
            Pedido padre: {pedido.pedidoPadreId}
          </p>
        )}
        <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
          Observaciones: {pedido.observaciones ?? "-"}
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {pedido.estado === "PENDIENTE_CONFIRMACION" && hasAnyRole("VENDEDOR", "ADMINISTRATIVO") && (
          <Button onClick={onConfirm}>Confirmar</Button>
        )}
        {pedido.estado === "PENDIENTE_PREPARACION" && hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO") && (
          <Button onClick={onDespachar}>Despachar</Button>
        )}
        {(pedido.estado === "PENDIENTE_ENTREGA" || pedido.estado === "EN_VIAJE") &&
          hasAnyRole("REPARTIDOR", "ADMINISTRATIVO") && (
            <Button onClick={onReagendar}>Reagendar</Button>
          )}
        {[
          "PENDIENTE_CONFIRMACION",
          "PENDIENTE_STOCK",
          "PENDIENTE_PREPARACION",
          "PENDIENTE_ENTREGA",
          "EN_VIAJE",
          "RE_AGENDADO",
        ].includes(pedido.estado) && (
          <Button variant="secondary" onClick={onRechazar} className="text-red-600 dark:text-red-400">
            Rechazar
          </Button>
        )}
        <Button variant="secondary" onClick={onVerHijos}>
          Ver hijos
        </Button>
        {(pedido.estado === "ENTREGADO" || pedido.estado === "ENTREGADO_PARCIAL") &&
          hasAnyRole("REPARTIDOR", "ADMINISTRATIVO") && (
            <Button variant="secondary" onClick={onSustituir}>
              Sustituir item
            </Button>
          )}
      </div>

      {hijosLoading && <p className="text-sm text-neutral-500">Cargando hijos...</p>}
      {hijosError && <p className="text-sm text-red-600 dark:text-red-400">{hijosError}</p>}
      {verHijosActivo && hijos && (
        <div>
          <h4 className="mb-2 text-xs font-semibold uppercase tracking-wide text-neutral-500">
            Pedidos hijos
          </h4>
          {hijos.length === 0 ? (
            <p className="text-sm text-neutral-500">Este pedido no tiene hijos.</p>
          ) : (
            <ul className="space-y-1.5">
              {hijos.map((hijo) => (
                <li key={hijo.id} className="flex items-center justify-between gap-3 text-sm">
                  <span className="font-medium text-neutral-800 dark:text-neutral-200">{hijo.numero}</span>
                  <EstadoBadge estado={hijo.estado} />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

function MarcarFaltanteModal({
  pedido,
  item,
  onClose,
  onConfirm,
}: {
  pedido: Pedido;
  item: PedidoItem;
  onClose: () => void;
  onConfirm: () => Promise<void> | void;
}) {
  const [cantidad, setCantidad] = useState("");
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    const cantidadNum = Number(cantidad);
    if (!cantidad || Number.isNaN(cantidadNum) || cantidadNum <= 0) {
      setError("Ingresá una cantidad mayor a cero.");
      return;
    }
    if (cantidadNum > item.cantidadReservada) {
      setError(`La cantidad no puede superar la reservada (${formatNumber(item.cantidadReservada)}).`);
      return;
    }
    if (!motivo.trim()) {
      setError("Ingresá el motivo del faltante.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost(`/api/pedidos/${pedido.id}/marcar-faltante`, {
        itemId: item.itemId,
        cantidad: cantidadNum,
        motivo: motivo.trim(),
      });
      await onConfirm();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title={`Marcar faltante · item #${item.itemId}`} onClose={onClose} width="sm">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="faltante-cantidad"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            Cantidad (máx. {formatNumber(item.cantidadReservada)})
          </label>
          <input
            id="faltante-cantidad"
            type="number"
            min="0.001"
            max={item.cantidadReservada}
            step="0.001"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
            autoFocus
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label
            htmlFor="faltante-motivo"
            className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
          >
            Motivo
          </label>
          <textarea
            id="faltante-motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            rows={3}
            required
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          />
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Guardando..." : "Marcar faltante"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function SustitucionModal({
  pedido,
  onClose,
  onConfirm,
}: {
  pedido: Pedido;
  onClose: () => void;
  onConfirm: () => Promise<void> | void;
}) {
  const lineasEntregadas = useMemo(
    () => pedido.items.filter((it) => it.cantidadEntregada > 0),
    [pedido.items]
  );

  const [itemOriginalId, setItemOriginalId] = useState<number | null>(
    lineasEntregadas[0]?.itemId ?? null
  );
  const [itemSustitutoId, setItemSustitutoId] = useState<number | null>(null);
  const [cantidad, setCantidad] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const lineaOriginal = lineasEntregadas.find((it) => it.itemId === itemOriginalId) ?? null;
  const cantidadMax = lineaOriginal?.cantidadEntregada ?? 0;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (itemOriginalId == null) {
      setError("Seleccioná el item entregado.");
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
      setError(`La cantidad no puede superar la entregada (${formatNumber(cantidadMax)}).`);
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
            Item entregado
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
            {lineasEntregadas.length === 0 && <option value="">Sin items entregados</option>}
            {lineasEntregadas.map((it) => (
              <option key={it.pedidoItemId} value={it.itemId}>
                #{it.itemId} — {formatNumber(it.cantidadEntregada)} entregada
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
              `/api/items?q=${encodeURIComponent(q)}&size=20`
            );
            return data.content.map((i) => ({
              id: i.id,
              label: `${i.sku} — ${i.nombre}`,
              sublabel: i.categoria ?? "",
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
          <Button type="submit" disabled={submitting || lineasEntregadas.length === 0}>
            {submitting ? "Guardando..." : "Registrar sustitución"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function NuevoPedidoForm({
  vendedorId,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  vendedorId: number;
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [fechaJornada, setFechaJornada] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [express, setExpress] = useState(false);
  const [categoria, setCategoria] = useState("");
  const [categorias, setCategorias] = useState<string[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [lineas, setLineas] = useState<FormItemRow[]>([{ key: Date.now(), itemId: null, itemLabel: null, cantidad: "", precioUnitario: "" }]);

  useEffect(() => {
    apiGet<string[]>("/api/items/categorias")
      .then(setCategorias)
      .catch(() => {
        // El select de categorías mostrará solo "Todas las categorías"
      });
  }, []);

  useEffect(() => {
    const params = new URLSearchParams({ size: "500" });
    if (categoria) params.set("categoria", categoria);
    apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`)
      .then((data) => setItems(data.content))
      .catch(() => {
        setItems([]);
      });
  }, [categoria]);

  function addLinea() {
    setLineas((prev) => [...prev, { key: Date.now() + Math.random(), itemId: null, itemLabel: null, cantidad: "", precioUnitario: "" }]);
  }

  function updateLinea<K extends keyof FormItemRow>(key: number, field: K, value: FormItemRow[K]) {
    setLineas((prev) => prev.map((l) => (l.key === key ? { ...l, [field]: value } : l)));
  }

  function removeLinea(key: number) {
    setLineas((prev) => prev.filter((l) => l.key !== key));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (clienteId == null) {
      setError("Seleccioná un cliente.");
      return;
    }
    const lineasValidas = lineas.filter((l) => l.itemId != null && Number(l.cantidad) > 0);
    if (lineasValidas.length === 0) {
      setError("Agregá al menos un item con cantidad mayor a cero.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/pedidos", {
        clienteId,
        vendedorId,
        fechaJornada: fechaJornada || undefined,
        observaciones: observaciones.trim() || undefined,
        express,
        items: lineasValidas.map((l) => ({
          itemId: l.itemId as number,
          cantidad: Number(l.cantidad),
          precioUnitario: Number(l.precioUnitario || 0),
        })),
      });
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Drawer title="Nuevo pedido" onClose={onClose} width="lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Combobox
            label="Cliente"
            placeholder="Buscar cliente..."
            required
            value={clienteId}
            onChange={setClienteId}
            search={async (q) => {
              const data = await apiGet<PageResponse<Cliente>>(
                `/api/clientes?q=${encodeURIComponent(q)}&size=20`
              );
              return data.content.map((c) => ({
                id: c.id,
                label: c.razonSocial,
                sublabel: [c.cuit, c.zonaNombre].filter(Boolean).join(" · "),
              }));
            }}
          />
          <div className="flex flex-col gap-1.5">
            <label htmlFor="fechaJornada" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Fecha de jornada
            </label>
            <input
              id="fechaJornada"
              type="date"
              value={fechaJornada}
              onChange={(e) => setFechaJornada(e.target.value)}
              className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
            />
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="observaciones" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Observaciones
          </label>
          <textarea
            id="observaciones"
            value={observaciones}
            onChange={(e) => setObservaciones(e.target.value)}
            rows={2}
            className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
          />
        </div>

        <label className="flex items-center gap-2 text-sm font-medium text-neutral-700 dark:text-neutral-300">
          <input
            type="checkbox"
            checked={express}
            onChange={(e) => setExpress(e.target.checked)}
            className="h-4 w-4 rounded border-neutral-300 text-blue-600 focus:ring-blue-500 dark:border-neutral-700 dark:bg-neutral-800"
          />
          Pedido express (prioridad en preparación y despacho)
        </label>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Items</span>
            <button
              type="button"
              onClick={addLinea}
              className="rounded-md border border-neutral-300 px-2.5 py-1 text-xs font-medium text-neutral-700 transition-colors hover:bg-neutral-100 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              + Agregar item
            </button>
          </div>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <label
              htmlFor="pedido-categoria"
              className="text-sm font-medium text-neutral-700 dark:text-neutral-300"
            >
              Categoría
            </label>
            <select
              id="pedido-categoria"
              value={categoria}
              onChange={(e) => {
                setCategoria(e.target.value);
              }}
              className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
            >
              <option value="">Todas las categorías</option>
              {categorias.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            {lineas.map((linea) => (
              <div key={linea.key} className="grid grid-cols-[1fr_5rem_6rem_auto] gap-2">
                <Combobox
                  key={linea.key}
                  placeholder="Buscar item..."
                  value={linea.itemId}
                  valueLabel={linea.itemLabel}
                  onChange={(id) => {
                    updateLinea(linea.key, "itemId", id);
                    if (id != null) {
                      const item = items.find((i) => i.id === id);
                      if (item) {
                        updateLinea(linea.key, "itemLabel", `${item.sku} — ${item.nombre}`);
                        updateLinea(linea.key, "precioUnitario", String(item.precioLista ?? 0));
                      } else {
                        updateLinea(linea.key, "itemLabel", null);
                      }
                    } else {
                      updateLinea(linea.key, "itemLabel", null);
                    }
                  }}
                  search={async (q) => {
                    const params = new URLSearchParams({ q, size: "20" });
                    if (categoria) params.set("categoria", categoria);
                    const data = await apiGet<PageResponse<Item>>(`/api/items?${params.toString()}`);
                    return data.content.map((i) => ({
                      id: i.id,
                      label: `${i.sku} — ${i.nombre}`,
                      sublabel: i.categoria ?? "",
                    }));
                  }}
                />
                <input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Cant."
                  value={linea.cantidad}
                  onChange={(e) => updateLinea(linea.key, "cantidad", e.target.value)}
                  className="rounded-md border border-neutral-300 bg-white px-2 py-1.5 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
                />
                <input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Precio"
                  value={linea.precioUnitario}
                  onChange={(e) => updateLinea(linea.key, "precioUnitario", e.target.value)}
                  className="rounded-md border border-neutral-300 bg-white px-2 py-1.5 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
                />
                <button
                  type="button"
                  onClick={() => removeLinea(linea.key)}
                  disabled={lineas.length === 1}
                  aria-label="Quitar item"
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
          <Button type="submit" disabled={submitting}>
            {submitting ? "Creando..." : "Crear pedido"}
          </Button>
        </div>
      </form>
    </Drawer>
  );
}
