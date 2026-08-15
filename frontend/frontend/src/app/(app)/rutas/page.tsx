"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import type {
  AsignarPedidosRequest,
  Cliente,
  EntregaRequest,
  EstadoRuta,
  PageResponse,
  Pedido,
  Remito,
  Ruta,
  RutaRequest,
  Usuario,
  Zona,
} from "@/lib/types";
import { formatDate, formatMoney, formatNumber } from "@/lib/format";
import { useAuth } from "@/lib/auth";
import EstadoBadge from "@/components/EstadoBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Combobox from "@/components/Combobox";
import { IconClose } from "@/components/icons";

const ESTADOS_RUTA: EstadoRuta[] = ["PLANIFICADA", "EN_CURSO", "FINALIZADA"];

const ESTADOS_DISPONIBLES = ["PENDIENTE_ENTREGA", "RE_AGENDADO"];

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

const RUTA_ESTADO_STYLES: Record<EstadoRuta, { badge: string; label: string }> = {
  PLANIFICADA: {
    badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
    label: "Planificada",
  },
  EN_CURSO: {
    badge: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300",
    label: "En curso",
  },
  FINALIZADA: {
    badge: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300",
    label: "Finalizada",
  },
};

function todayStr(): string {
  const d = new Date();
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function pedidoBultos(pedido: Pedido | undefined): number {
  if (!pedido) return 0;
  return pedido.items.reduce((sum, item) => sum + item.cantidadReservada, 0);
}

function bultosDeIds(ids: number[], pedidosById: Map<number, Pedido>): number {
  return ids.reduce((sum, id) => sum + pedidoBultos(pedidosById.get(id)), 0);
}

function RutaEstadoBadge({ estado }: { estado: EstadoRuta }) {
  const { badge, label } = RUTA_ESTADO_STYLES[estado] ?? {
    badge: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300",
    label: estado,
  };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ${badge}`}>
      {label}
    </span>
  );
}

function LoadIndicator({ load, capacidad }: { load: number; capacidad: number }) {
  if (capacidad <= 0) {
    return <span className="text-xs text-neutral-500">Sin límite</span>;
  }
  const ratio = load / capacidad;
  const excede = ratio > 1;
  const barColor = excede ? "bg-red-500" : ratio >= 0.8 ? "bg-amber-500" : "bg-emerald-500";
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-2">
        <span
          className={`text-xs ${
            excede ? "font-medium text-red-600 dark:text-red-400" : "text-neutral-600 dark:text-neutral-400"
          }`}
        >
          {formatNumber(load)} / {formatNumber(capacidad)} bultos
        </span>
        {excede && (
          <span className="rounded-full bg-red-100 px-1.5 py-0.5 text-[10px] font-medium text-red-700 dark:bg-red-900/40 dark:text-red-300">
            excede capacidad
          </span>
        )}
      </div>
      <div className="h-1.5 w-28 overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-700">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${Math.min(100, ratio * 100)}%` }} />
      </div>
    </div>
  );
}

function CapacityProjection({
  current,
  added,
  capacidad,
}: {
  current: number;
  added: number;
  capacidad: number;
}) {
  const total = current + added;
  if (capacidad <= 0) {
    return <p className="text-xs text-neutral-500">Carga proyectada: {formatNumber(total)} bultos (sin límite)</p>;
  }
  const excede = total > capacidad;
  const barColor = excede ? "bg-red-500" : total / capacidad >= 0.8 ? "bg-amber-500" : "bg-emerald-500";
  return (
    <div className="flex flex-col gap-1">
      <p
        className={`text-xs font-medium ${
          excede ? "text-red-600 dark:text-red-400" : "text-neutral-700 dark:text-neutral-300"
        }`}
      >
        Carga proyectada: {formatNumber(current)} + {formatNumber(added)} = {formatNumber(total)} de{" "}
        {formatNumber(capacidad)} bultos{excede ? " · excede la capacidad" : ""}
      </p>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-700">
        <div
          className={`h-full rounded-full ${barColor}`}
          style={{ width: `${Math.min(100, (total / capacidad) * 100)}%` }}
        />
      </div>
    </div>
  );
}

interface DeliveryInfo {
  message: string;
  hijos: Pedido[];
  remitos: Remito[];
}

export default function RutasPage() {
  const { user } = useAuth();
  const isAdmin = user?.roles.includes("ADMINISTRATIVO") ?? false;

  const [rutas, setRutas] = useState<Ruta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [fecha, setFecha] = useState<string>(todayStr);
  const [estado, setEstado] = useState<string>("");

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [deliveryInfo, setDeliveryInfo] = useState<Record<number, DeliveryInfo>>({});

  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [zonas, setZonas] = useState<Zona[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);

  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadRutas = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (fecha) params.set("fecha", fecha);
      else if (estado) params.set("estado", estado);
      const url = params.toString() ? `/api/rutas?${params.toString()}` : "/api/rutas";
      const data = await apiGet<Ruta[]>(url);
      setRutas(fecha && estado ? data.filter((r) => r.estado === estado) : data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setLoading(false);
    }
  }, [fecha, estado]);

  const loadPedidos = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Pedido>>("/api/pedidos?size=500");
      setPedidos(data.content);
    } catch {
      // El detalle y los selectores mostrarán filas incompletas o el error al usarlos
    }
  }, []);

  const loadZonas = useCallback(async () => {
    try {
      setZonas(await apiGet<Zona[]>("/api/zonas"));
    } catch {
      // El formulario de alta mostrará el error al intentar usarlo
    }
  }, []);

  const loadClientes = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Cliente>>("/api/clientes?size=500");
      setClientes(data.content);
    } catch {
      // Los selectores de pedidos se muestran sin zona de cliente al fallar
    }
  }, []);

  const loadUsuarios = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Usuario>>("/api/usuarios?size=500");
      setUsuarios(data.content);
    } catch {
      // El formulario de alta mostrará el error al intentar usarlo
    }
  }, []);

  useEffect(() => {
    loadRutas();
  }, [loadRutas]);

  useEffect(() => {
    loadPedidos();
    loadZonas();
    loadClientes();
  }, [loadPedidos, loadZonas, loadClientes]);

  useEffect(() => {
    if (isAdmin) loadUsuarios();
  }, [isAdmin, loadUsuarios]);

  const pedidosById = useMemo(() => new Map(pedidos.map((p) => [p.id, p])), [pedidos]);

  const clientesById = useMemo(() => new Map(clientes.map((c) => [c.id, c])), [clientes]);

  const zonasById = useMemo(() => new Map(zonas.map((z) => [z.id, z.nombre])), [zonas]);

  const usuariosById = useMemo(() => new Map(usuarios.map((u) => [u.id, u.nombre])), [usuarios]);

  const disponibles = useMemo(
    () => pedidos.filter((p) => ESTADOS_DISPONIBLES.includes(p.estado)),
    [pedidos]
  );

  const selectedRuta = useMemo(
    () => (selectedId != null ? rutas.find((r) => r.id === selectedId) ?? null : null),
    [rutas, selectedId]
  );

  async function iniciarJornada(ruta: Ruta) {
    try {
      await apiPost<Ruta>(`/api/rutas/${ruta.id}/iniciar`);
      await loadRutas();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function cerrarJornada(ruta: Ruta) {
    try {
      await apiPost<Ruta>(`/api/rutas/${ruta.id}/cerrar`);
      await loadRutas();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  async function handleEntregaRegistrada(pedidoId: number, result: Pedido) {
    const info: DeliveryInfo = {
      message:
        result.estado === "ENTREGADO_PARCIAL"
          ? "Entrega parcial registrada — Se generó pedido hijo, ver en Pedidos"
          : "Entrega registrada",
      hijos: [],
      remitos: [],
    };
    if (result.estado === "ENTREGADO_PARCIAL") {
      try {
        info.hijos = await apiGet<Pedido[]>(`/api/pedidos/${pedidoId}/hijos`);
      } catch {
        // No bloquea la visualización de la entrega registrada
      }
    }
    try {
      info.remitos = await apiGet<Remito[]>(`/api/remitos?pedidoId=${pedidoId}`);
    } catch {
      // No bloquea la visualización de la entrega registrada
    }
    setDeliveryInfo((prev) => ({ ...prev, [pedidoId]: info }));
    await Promise.all([loadPedidos(), loadRutas()]);
  }

  function handleSelect(id: number) {
    setSelectedId((prev) => (prev === id ? null : id));
    setDeliveryInfo({});
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Rutas + Entregas</h1>
        {isAdmin && (
          <Button
            onClick={() => {
              setFormError(null);
              setShowForm(true);
            }}
          >
            Nueva ruta
          </Button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label htmlFor="fecha" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Fecha
          </label>
          <input
            id="fecha"
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>
        <div className="flex items-center gap-2">
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
            {ESTADOS_RUTA.map((e) => (
              <option key={e} value={e}>
                {e}
              </option>
            ))}
          </select>
        </div>
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
                  <th className="px-4 py-3 font-medium">Ruta</th>
                  <th className="px-4 py-3 font-medium">Zona</th>
                  <th className="px-4 py-3 font-medium">Repartidor</th>
                  <th className="px-4 py-3 font-medium">Fecha jornada</th>
                  <th className="px-4 py-3 font-medium">Estado</th>
                  <th className="px-4 py-3 font-medium">Capacidad</th>
                  <th className="px-4 py-3 font-medium text-right">Pedidos</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {rutas.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-neutral-500">
                      No hay rutas para mostrar
                    </td>
                  </tr>
                )}
                {rutas.map((ruta) => {
                  const load = bultosDeIds(ruta.pedidoIds, pedidosById);
                  return (
                    <tr
                      key={ruta.id}
                      className={`transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50 ${
                        selectedId === ruta.id ? "bg-blue-50 dark:bg-blue-950/40" : ""
                      }`}
                    >
                      <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">Ruta #{ruta.id}</td>
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
                        {zonasById.get(ruta.zonaId) ?? `Zona #${ruta.zonaId}`}
                      </td>
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
                        {usuariosById.get(ruta.repartidorId) ?? `#${ruta.repartidorId}`}
                      </td>
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{formatDate(ruta.fechaJornada)}</td>
                      <td className="px-4 py-3">
                        <RutaEstadoBadge estado={ruta.estado} />
                      </td>
                      <td className="px-4 py-3">
                        <LoadIndicator load={load} capacidad={ruta.capacidadBultos} />
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-neutral-700 dark:text-neutral-300">
                        {ruta.pedidoIds.length}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button variant="secondary" onClick={() => handleSelect(ruta.id)}>
                          {selectedId === ruta.id ? "Ocultar" : "Detalle"}
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

      {selectedRuta && (
        <RutaDetalle
          ruta={selectedRuta}
          pedidosById={pedidosById}
          clientesById={clientesById}
          zonaNombre={zonasById.get(selectedRuta.zonaId)}
          repartidorNombre={usuariosById.get(selectedRuta.repartidorId)}
          disponibles={disponibles}
          deliveryInfo={deliveryInfo}
          onIniciar={() => iniciarJornada(selectedRuta)}
          onCerrar={() => cerrarJornada(selectedRuta)}
          onAsignada={async () => {
            await Promise.all([loadPedidos(), loadRutas()]);
          }}
          onEntregaRegistrada={handleEntregaRegistrada}
        />
      )}

      {showForm && (
        <NuevaRutaForm
          zonas={zonas}
          disponibles={disponibles}
          clientesById={clientesById}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setShowForm(false)}
          onCreated={async () => {
            setShowForm(false);
            await Promise.all([loadPedidos(), loadRutas()]);
          }}
        />
      )}
    </div>
  );
}

function RutaDetalle({
  ruta,
  pedidosById,
  clientesById,
  zonaNombre,
  repartidorNombre,
  disponibles,
  deliveryInfo,
  onIniciar,
  onCerrar,
  onAsignada,
  onEntregaRegistrada,
}: {
  ruta: Ruta;
  pedidosById: Map<number, Pedido>;
  clientesById: Map<number, Cliente>;
  zonaNombre: string | undefined;
  repartidorNombre: string | undefined;
  disponibles: Pedido[];
  deliveryInfo: Record<number, DeliveryInfo>;
  onIniciar: () => Promise<void>;
  onCerrar: () => Promise<void>;
  onAsignada: () => Promise<void>;
  onEntregaRegistrada: (pedidoId: number, result: Pedido) => Promise<void>;
}) {
  const [asignarOpen, setAsignarOpen] = useState(false);

  const asignados = ruta.pedidoIds.map((id) => pedidosById.get(id)).filter((p) => p !== undefined) as Pedido[];
  const enViaje = asignados.filter((p) => p.estado === "EN_VIAJE");
  const routeLoad = bultosDeIds(ruta.pedidoIds, pedidosById);

  return (
    <section className="flex flex-col gap-4 rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
        <div className="flex items-center gap-3">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Ruta #{ruta.id}</h2>
          <RutaEstadoBadge estado={ruta.estado} />
        </div>
        <div className="text-sm text-neutral-500">
          Zona: {zonaNombre ?? `#${ruta.zonaId}`} · Repartidor: {repartidorNombre ?? `#${ruta.repartidorId}`} ·{" "}
          Jornada: {formatDate(ruta.fechaJornada)}
        </div>
      </div>

      <div className="px-5">
        <div className="mb-2 flex items-center justify-between">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">Pedidos asignados</h3>
          <LoadIndicator load={routeLoad} capacidad={ruta.capacidadBultos} />
        </div>
        <div className="overflow-hidden rounded-md border border-neutral-200 dark:border-neutral-800">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-white text-xs uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900">
                <th className="px-3 py-2 font-medium text-left">Número</th>
                <th className="px-3 py-2 font-medium text-left">Cliente</th>
                <th className="px-3 py-2 font-medium text-left">Estado</th>
                <th className="px-3 py-2 font-medium text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
              {ruta.pedidoIds.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-3 py-5 text-center text-neutral-500">
                    Esta ruta no tiene pedidos asignados
                  </td>
                </tr>
              )}
              {ruta.pedidoIds.map((id) => {
                const pedido = pedidosById.get(id);
                return (
                  <tr key={id}>
                    <td className="px-3 py-2 font-medium text-neutral-900 dark:text-neutral-100">
                      {pedido?.numero ?? `Pedido #${id}`}
                    </td>
                    <td className="px-3 py-2 text-neutral-600 dark:text-neutral-400">
                      {pedido ? `#${pedido.clienteId}` : "-"}
                    </td>
                    <td className="px-3 py-2">
                      {pedido ? <EstadoBadge estado={pedido.estado} /> : <span className="text-xs text-neutral-400">no encontrado</span>}
                    </td>
                    <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                      {pedido ? formatMoney(pedido.total) : "-"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {ruta.estado === "PLANIFICADA" && (
        <div className="flex flex-col gap-3 px-5 pb-5">
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => setAsignarOpen((prev) => !prev)}>
              {asignarOpen ? "Cerrar asignación" : "Asignar pedidos"}
            </Button>
            <Button variant="secondary" onClick={onIniciar}>
              Iniciar jornada
            </Button>
          </div>

          {asignarOpen && (
            <AsignarPedidosPanel
              ruta={ruta}
              pedidosById={pedidosById}
              disponibles={disponibles}
              clientesById={clientesById}
              onCancel={() => setAsignarOpen(false)}
              onAsignada={async () => {
                setAsignarOpen(false);
                await onAsignada();
              }}
            />
          )}
        </div>
      )}

      {ruta.estado === "EN_CURSO" && (
        <div className="flex flex-col gap-4 px-5 pb-5">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">Entregas en curso</h3>
            <Button variant="secondary" onClick={onCerrar}>
              Cerrar jornada
            </Button>
          </div>

          {Object.keys(deliveryInfo).length > 0 && (
            <div className="flex flex-col gap-2">
              {Object.entries(deliveryInfo).map(([pedidoId, info]) => (
                <div
                  key={pedidoId}
                  className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300"
                >
                  <p>
                    Pedido #{pedidoId}: {info.message}
                  </p>
                  {info.remitos.length > 0 && (
                    <p className="mt-1">
                      {info.remitos.length > 1 ? "Remitos" : "Remito"}{" "}
                      {info.remitos.map((r) => r.numero).join(", ")} generado
                      {info.remitos.length > 1 ? "s" : ""}
                    </p>
                  )}
                  {info.hijos.length > 0 && (
                    <ul className="mt-2 flex flex-col gap-1.5">
                      {info.hijos.map((hijo) => (
                        <li key={hijo.id} className="flex items-center justify-between gap-3">
                          <span className="font-medium">{hijo.numero}</span>
                          <EstadoBadge estado={hijo.estado} />
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </div>
          )}

          {enViaje.length === 0 ? (
            <p className="text-sm text-neutral-500">No hay pedidos en viaje para registrar entregas.</p>
          ) : (
            <div className="flex flex-col gap-4">
              {enViaje.map((pedido) => (
                <PedidoEntregaCard key={pedido.id} pedido={pedido} onRegistered={onEntregaRegistrada} />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function AsignarPedidosPanel({
  ruta,
  pedidosById,
  disponibles,
  clientesById,
  onCancel,
  onAsignada,
}: {
  ruta: Ruta;
  pedidosById: Map<number, Pedido>;
  disponibles: Pedido[];
  clientesById: Map<number, Cliente>;
  onCancel: () => void;
  onAsignada: () => Promise<void>;
}) {
  const [selected, setSelected] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const candidates = useMemo(
    () =>
      disponibles.filter(
        (p) => !ruta.pedidoIds.includes(p.id) && clientesById.get(p.clienteId)?.zonaId === ruta.zonaId
      ),
    [disponibles, clientesById, ruta]
  );

  const allSelected = candidates.length > 0 && candidates.every((p) => selected.includes(p.id));
  const currentLoad = bultosDeIds(ruta.pedidoIds, pedidosById);
  const addedLoad = candidates
    .filter((p) => selected.includes(p.id))
    .reduce((sum, p) => sum + pedidoBultos(p), 0);

  function toggle(id: number) {
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  function toggleSelectAll() {
    setSelected(allSelected ? [] : candidates.map((p) => p.id));
  }

  async function handleSave() {
    if (submitting) return;
    if (selected.length === 0) {
      setError("Seleccioná al menos un pedido.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await apiPost<Ruta>(`/api/rutas/${ruta.id}/pedidos`, { pedidoIds: selected } satisfies AsignarPedidosRequest);
      await onAsignada();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="rounded-md border border-neutral-200 p-4 dark:border-neutral-800">
      <div className="mb-3 flex items-center justify-between">
        <h4 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
          Pedidos despachados disponibles ({candidates.length})
        </h4>
        <div className="flex items-center gap-2">
          {candidates.length > 0 && (
            <button
              type="button"
              onClick={toggleSelectAll}
              className="text-xs font-medium text-blue-600 hover:underline dark:text-blue-400"
            >
              {allSelected ? "Quitar todos" : "Seleccionar todos"}
            </button>
          )}
          <button
            type="button"
            onClick={onCancel}
            aria-label="Cerrar"
            className="rounded-md p-1.5 text-neutral-500 hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            <IconClose />
          </button>
        </div>
      </div>

      {candidates.length === 0 ? (
        <p className="text-sm text-neutral-500">No hay pedidos despachados disponibles para esta zona.</p>
      ) : (
        <div className="max-h-72 space-y-1.5 overflow-y-auto">
          {candidates.map((p) => (
            <label
              key={p.id}
              className="flex cursor-pointer items-center gap-3 rounded-md border border-neutral-200 px-3 py-2 text-sm font-medium text-neutral-800 transition-colors hover:bg-neutral-50 dark:border-neutral-800 dark:text-neutral-200 dark:hover:bg-neutral-800/50"
            >
              <input
                type="checkbox"
                checked={selected.includes(p.id)}
                onChange={() => toggle(p.id)}
                className="h-4 w-4 rounded border-neutral-300 accent-blue-600"
              />
              <span>
                {p.numero} · {clientesById.get(p.clienteId)?.razonSocial ?? `cliente #${p.clienteId}`}
              </span>
            </label>
          ))}
        </div>
      )}

      {selected.length > 0 && (
        <div className="mt-3">
          <CapacityProjection current={currentLoad} added={addedLoad} capacidad={ruta.capacidadBultos} />
        </div>
      )}

      {error && <div className="mt-3"><ErrorBox message={error} /></div>}

      <div className="mt-3 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="button" onClick={handleSave} disabled={submitting || candidates.length === 0}>
          {submitting ? "Asignando..." : "Asignar"}
        </Button>
      </div>
    </div>
  );
}

function PedidoEntregaCard({
  pedido,
  onRegistered,
}: {
  pedido: Pedido;
  onRegistered: (pedidoId: number, result: Pedido) => Promise<void>;
}) {
  const [cantidades, setCantidades] = useState<Record<number, string>>(() =>
    Object.fromEntries(pedido.items.map((it) => [it.pedidoItemId, String(it.cantidadReservada)]))
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function setCantidad(pedidoItemId: number, value: string) {
    setCantidades((prev) => ({ ...prev, [pedidoItemId]: value }));
  }

  async function handleSubmit() {
    if (submitting) return;
    setError(null);
    const entregas = pedido.items.map((it) => ({
      pedidoItemId: it.pedidoItemId,
      cantidadEntregada: Number(cantidades[it.pedidoItemId] ?? "0"),
    }));
    setSubmitting(true);
    try {
      const result = await apiPost<Pedido>(`/api/pedidos/${pedido.id}/entregas`, {
        entregas,
      } satisfies EntregaRequest);
      await onRegistered(pedido.id, result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="rounded-md border border-neutral-200 dark:border-neutral-800">
      <div className="flex items-center justify-between gap-3 border-b border-neutral-200 px-4 py-2.5 dark:border-neutral-800">
        <span className="font-medium text-neutral-900 dark:text-neutral-100">{pedido.numero}</span>
        <EstadoBadge estado={pedido.estado} />
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-neutral-200 text-xs uppercase text-neutral-500 dark:border-neutral-800">
              <th className="px-4 py-2 font-medium text-left">Item</th>
              <th className="px-4 py-2 font-medium text-right">Pedida</th>
              <th className="px-4 py-2 font-medium text-right">Reservada</th>
              <th className="px-4 py-2 font-medium text-right">Entregada</th>
              <th className="px-4 py-2 font-medium text-right">Precio</th>
              <th className="px-4 py-2 font-medium text-right">A entregar</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
            {pedido.items.map((it) => (
              <tr key={it.pedidoItemId}>
                <td className="px-4 py-2 text-neutral-700 dark:text-neutral-300">#{it.itemId}</td>
                <td className="px-4 py-2 text-right text-neutral-700 dark:text-neutral-300">
                  {formatNumber(it.cantidadPedida)}
                </td>
                <td className="px-4 py-2 text-right text-neutral-700 dark:text-neutral-300">
                  {formatNumber(it.cantidadReservada)}
                </td>
                <td className="px-4 py-2 text-right text-neutral-700 dark:text-neutral-300">
                  {formatNumber(it.cantidadEntregada)}
                </td>
                <td className="px-4 py-2 text-right text-neutral-700 dark:text-neutral-300">
                  {formatMoney(it.precioUnitario)}
                </td>
                <td className="px-4 py-2">
                  <div className="flex justify-end">
                    <input
                      type="number"
                      min="0"
                      max={it.cantidadReservada}
                      step="0.001"
                      value={cantidades[it.pedidoItemId] ?? "0"}
                      onChange={(e) => setCantidad(it.pedidoItemId, e.target.value)}
                      className="w-24 rounded-md border border-neutral-300 bg-white px-2 py-1 text-right text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
                    />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-end gap-3 px-4 py-3">
        {error && <ErrorBox message={error} />}
        <Button onClick={handleSubmit} disabled={submitting || pedido.items.length === 0}>
          {submitting ? "Registrando..." : "Registrar entrega"}
        </Button>
      </div>
    </div>
  );
}

function NuevaRutaForm({
  zonas,
  disponibles,
  clientesById,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  zonas: Zona[];
  disponibles: Pedido[];
  clientesById: Map<number, Cliente>;
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [zonaId, setZonaId] = useState("");
  const [repartidorId, setRepartidorId] = useState<number | null>(null);
  const [fechaJornada, setFechaJornada] = useState(todayStr);
  const [pedidoIds, setPedidoIds] = useState<number[]>([]);
  const [capacidadBultos, setCapacidadBultos] = useState("");

  const zonaNum = zonaId ? Number(zonaId) : null;

  const candidatos = useMemo(
    () =>
      disponibles.filter((p) => zonaNum != null && clientesById.get(p.clienteId)?.zonaId === zonaNum),
    [disponibles, clientesById, zonaNum]
  );

  function togglePedido(id: number) {
    setPedidoIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    if (!zonaId || repartidorId == null || !fechaJornada) {
      setError("Completá zona, repartidor y fecha.");
      return;
    }
    if (pedidoIds.length === 0) {
      setError("Seleccioná al menos un pedido.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/rutas", {
        zonaId: Number(zonaId),
        repartidorId,
        fechaJornada,
        pedidoIds,
        capacidadBultos: capacidadBultos.trim() === "" ? 0 : Number(capacidadBultos),
      } satisfies RutaRequest);
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Nueva ruta" onClose={onClose} width="lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Zona</label>
              <select
                value={zonaId}
                onChange={(e) => setZonaId(e.target.value)}
                className={INPUT_CLASS}
              >
                <option value="">Seleccionar...</option>
                {zonas.map((z) => (
                  <option key={z.id} value={z.id}>
                    {z.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Combobox
                label="Repartidor"
                placeholder="Buscar repartidor..."
                required
                value={repartidorId}
                onChange={setRepartidorId}
                search={async (q) => {
                  const data = await apiGet<PageResponse<Usuario>>(
                    `/api/usuarios?q=${encodeURIComponent(q)}&size=20`
                  );
                  return data.content
                    .filter((u) => u.roles.includes("REPARTIDOR"))
                    .map((u) => ({
                      id: u.id,
                      label: u.nombre,
                      sublabel: u.email,
                    }));
                }}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Fecha jornada</label>
              <input
                type="date"
                value={fechaJornada}
                onChange={(e) => setFechaJornada(e.target.value)}
                className={INPUT_CLASS}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Capacidad (bultos)</label>
              <input
                type="number"
                min="0"
                step="1"
                value={capacidadBultos}
                onChange={(e) => setCapacidadBultos(e.target.value)}
                placeholder="Sin límite"
                className={INPUT_CLASS}
              />
            </div>
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">Pedidos</span>
              <span className="text-xs text-neutral-500">{pedidoIds.length} seleccionados</span>
            </div>
            <p className="mb-2 text-xs text-neutral-500">Solo pedidos despachados (PENDIENTE_ENTREGA) o re-agendados.</p>
            {candidatos.length === 0 ? (
              <p className="text-sm text-neutral-500">No hay pedidos despachados disponibles para esta zona.</p>
            ) : (
              <div className="max-h-56 space-y-1.5 overflow-y-auto rounded-md border border-neutral-200 p-2 dark:border-neutral-800">
                {candidatos.map((p) => (
                  <label
                    key={p.id}
                    className="flex cursor-pointer items-center gap-3 rounded-md border border-neutral-200 px-3 py-2 text-sm font-medium text-neutral-800 transition-colors hover:bg-neutral-50 dark:border-neutral-800 dark:text-neutral-200 dark:hover:bg-neutral-800/50"
                  >
                    <input
                      type="checkbox"
                      checked={pedidoIds.includes(p.id)}
                      onChange={() => togglePedido(p.id)}
                      className="h-4 w-4 rounded border-neutral-300 accent-blue-600"
                    />
                    {p.numero} · {clientesById.get(p.clienteId)?.razonSocial ?? `cliente #${p.clienteId}`}
                  </label>
                ))}
              </div>
            )}
          </div>

          {error && <ErrorBox message={error} />}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Creando..." : "Crear ruta"}
            </Button>
          </div>
        </form>
    </Modal>
  );
}
