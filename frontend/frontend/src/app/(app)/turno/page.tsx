"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type {
  Cliente,
  EntregaRequest,
  PageResponse,
  Pedido,
  Ruta,
  Zona,
} from "@/lib/types";
import { formatDate, formatMoney, formatNumber } from "@/lib/format";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import ConfirmacionFrictionada from "@/components/ConfirmacionFrictionada";
import SwipeButton from "@/components/SwipeButton";

const ALLOWED_ROLES = ["REPARTIDOR", "ADMINISTRATIVO"];

export default function TurnoPage() {
  const { user } = useAuth();

  const canView = user ? user.roles.some((r) => ALLOWED_ROLES.includes(r)) : false;

  const [rutas, setRutas] = useState<Ruta[]>([]);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [zonas, setZonas] = useState<Zona[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [selectedRutaId, setSelectedRutaId] = useState<number | null>(null);
  const didAutoSelect = useRef(false);
  const [queueIndex, setQueueIndex] = useState(0);

  const [parcialOpen, setParcialOpen] = useState(false);
  const [rechazarOpen, setRechazarOpen] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [alertaAdicionalDismissed, setAlertaAdicionalDismissed] = useState(false);

  const repartidorId = user?.usuarioId;

  const loadRutas = useCallback(async () => {
    try {
      if (repartidorId == null) return;
      const params = new URLSearchParams();
      params.set("repartidorId", String(repartidorId));
      setRutas(await apiGet<Ruta[]>(`/api/rutas?${params.toString()}`));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }, [repartidorId]);

  const loadPedidos = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Pedido>>("/api/pedidos?size=500");
      setPedidos(data.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    }
  }, []);

  const loadClientes = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Cliente>>("/api/clientes?size=500");
      setClientes(data.content);
    } catch {
      // El nombre del cliente es opcional en la tarjeta
    }
  }, []);

  const loadZonas = useCallback(async () => {
    try {
      setZonas(await apiGet<Zona[]>("/api/zonas"));
    } catch {
      // El nombre de la zona es opcional en el encabezado
    }
  }, []);

  const refresh = useCallback(async () => {
    setError(null);
    setSuccess(null);
    setParcialOpen(false);
    setRechazarOpen(false);
    setAlertaAdicionalDismissed(false);
    await Promise.all([loadPedidos(), loadRutas()]);
    setQueueIndex(0);
  }, [loadPedidos, loadRutas]);

  useEffect(() => {
    if (repartidorId == null) return;
    let active = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        await Promise.all([loadRutas(), loadPedidos(), loadClientes(), loadZonas()]);
      } catch {
        // los loaders ya capturan el error
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [repartidorId, loadRutas, loadPedidos, loadClientes, loadZonas]);

  useEffect(() => {
    if (didAutoSelect.current || rutas.length === 0 || selectedRutaId != null) return;
    const enCurso = rutas.filter((r) => r.estado === "EN_CURSO");
    const pick =
      enCurso.length > 0
        ? [...enCurso].sort((a, b) =>
            (b.fechaJornada || "").localeCompare(a.fechaJornada || "")
          )[0]
        : (rutas.find((r) => r.estado === "PLANIFICADA") ?? null);
    if (pick) {
      didAutoSelect.current = true;
      setSelectedRutaId(pick.id);
      setQueueIndex(0);
    }
  }, [rutas, selectedRutaId]);

  const pedidosById = useMemo(
    () => new Map(pedidos.map((p) => [p.id, p])),
    [pedidos]
  );
  const clientesById = useMemo(
    () => new Map(clientes.map((c) => [c.id, c])),
    [clientes]
  );
  const zonasById = useMemo(
    () => new Map(zonas.map((z) => [z.id, z.nombre])),
    [zonas]
  );

  const selectedRuta = useMemo(
    () =>
      selectedRutaId != null
        ? (rutas.find((r) => r.id === selectedRutaId) ?? null)
        : null,
    [rutas, selectedRutaId]
  );

  const routePedidos = useMemo(() => {
    if (!selectedRuta) return [] as Pedido[];
    return selectedRuta.pedidoIds
      .map((id) => pedidosById.get(id))
      .filter((p): p is Pedido => p !== undefined);
  }, [selectedRuta, pedidosById]);

  const enViaje = useMemo(
    () => routePedidos.filter((p) => p.estado === "EN_VIAJE"),
    [routePedidos]
  );

  const entregadosCount = useMemo(
    () =>
      routePedidos.filter(
        (p) => p.estado === "ENTREGADO" || p.estado === "ENTREGADO_PARCIAL"
      ).length,
    [routePedidos]
  );

  const enViajeClienteIds = useMemo(
    () => new Set(enViaje.map((p) => p.clienteId)),
    [enViaje]
  );

  const pedidosAdicionales = useMemo(
    () =>
      pedidos.filter(
        (p) =>
          p.estado === "PENDIENTE_CONFIRMACION" && enViajeClienteIds.has(p.clienteId)
      ),
    [pedidos, enViajeClienteIds]
  );

  const pedidosAdicionalesPorCliente = useMemo(() => {
    const map = new Map<number, Pedido[]>();
    for (const p of pedidosAdicionales) {
      const arr = map.get(p.clienteId) ?? [];
      arr.push(p);
      map.set(p.clienteId, arr);
    }
    return map;
  }, [pedidosAdicionales]);

  const current = enViaje.length > 0 ? enViaje[Math.min(queueIndex, enViaje.length - 1)] : null;

  const zonaNombre = selectedRuta ? zonasById.get(selectedRuta.zonaId) : undefined;

  const needPicker = !loading && !selectedRutaId;

  if (!canView) {
    return (
      <div className="mx-auto flex min-h-[60vh] w-full max-w-md flex-col items-center justify-center gap-4 px-4">
        <h1 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
          Turno de repartidor
        </h1>
        <p className="text-center text-sm text-neutral-500">
          Esta vista es solo para usuarios repartidor o administrativo.
        </p>
        <Link href="/" className="text-sm font-medium text-blue-600 hover:underline dark:text-blue-400">
          Volver al panel
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-4 px-4 pb-52 pt-4 sm:pt-8">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">Turno</h1>
        <p className="text-sm text-neutral-500">Entregas del día, una por una.</p>
      </div>

      {error && <ErrorBox message={error} />}
      {success && (
        <div
          role="status"
          className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300"
        >
          {success}
        </div>
      )}

      {loading ? (
        <Loading label="Cargando turno..." />
      ) : needPicker ? (
        <RoutePicker rutas={rutas} onSelect={(id) => setSelectedRutaId(id)} />
      ) : selectedRuta ? (
        <>
          <TurnoHeader
            ruta={selectedRuta}
            zonaNombre={zonaNombre}
            entregados={entregadosCount}
            enRuta={enViaje.length}
            total={routePedidos.length}
          />

          {selectedRuta.estado === "PLANIFICADA" && (
            <div className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
              <p className="text-sm text-neutral-600 dark:text-neutral-400">
                Esta ruta aún no inició. Cuando estés listo, iniciá la jornada para
                empezar a entregar.
              </p>
              <Button
                className="h-14 py-4 text-base"
                disabled={mutating}
                onClick={async () => {
                  if (!selectedRuta) return;
                  setMutating(true);
                  setError(null);
                  try {
                    await apiPost<Ruta>(`/api/rutas/${selectedRuta.id}/iniciar`);
                    didAutoSelect.current = true;
                    await refresh();
                  } catch (err) {
                    setError(err instanceof Error ? err.message : "Error inesperado");
                  } finally {
                    setMutating(false);
                  }
                }}
              >
                {mutating ? "Iniciando..." : "Iniciar jornada"}
              </Button>
            </div>
          )}

          {selectedRuta.estado === "EN_CURSO" && (
            <>
              {pedidosAdicionalesPorCliente.size > 0 && !alertaAdicionalDismissed && (
                <div className="flex flex-col gap-2 rounded-xl border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-950/40">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-col gap-1.5">
                      {[...pedidosAdicionalesPorCliente.entries()].map(([clienteId, adicionales]) => (
                        <p
                          key={clienteId}
                          className="text-sm text-blue-800 dark:text-blue-300"
                        >
                          {clientesById.get(clienteId)?.razonSocial ?? `Cliente #${clienteId}`}{" "}
                          tiene un pedido adicional cargado hoy:{" "}
                          {adicionales.map((p) => p.numero).join(", ")}
                        </p>
                      ))}
                      <p className="text-xs text-blue-600 dark:text-blue-400">
                        No es entregable en esta ruta; coordiná el envío por separado.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setAlertaAdicionalDismissed(true)}
                      aria-label="Descartar alerta de pedidos adicionales"
                      className="shrink-0 rounded-md p-1.5 text-blue-700 transition-colors hover:bg-blue-100 dark:text-blue-300 dark:hover:bg-blue-900/40"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                      >
                        <path
                          fillRule="evenodd"
                          d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </button>
                  </div>
                </div>
              )}
              {current ? (
                <>
                  <PedidoCard
                    pedido={current}
                    cliente={clientesById.get(current.clienteId)}
                    clienteNombre={clientesById.get(current.clienteId)?.razonSocial}
                    posicion={enViaje.indexOf(current) + 1}
                    total={enViaje.length}
                    paradasRestantes={enViaje
                      .slice(queueIndex + 1)
                      .map((p) => ({
                        numero: p.numero,
                        clienteNombre:
                          clientesById.get(p.clienteId)?.razonSocial ??
                          `Cliente #${p.clienteId}`,
                      }))}
                  />

                  {queueIndex < enViaje.length - 1 && (
                    <Button
                      variant="secondary"
                      className="h-14 py-4 text-base"
                      onClick={() => setQueueIndex((i) => i + 1)}
                    >
                      Siguiente
                    </Button>
                  )}
                </>
              ) : (
                <div className="flex flex-col items-center gap-3 rounded-xl border border-neutral-200 bg-white p-8 text-center dark:border-neutral-800 dark:bg-neutral-900">
                  <p className="text-base font-medium text-neutral-900 dark:text-neutral-100">
                    ¡Ruta entregada!
                  </p>
                  <p className="text-sm text-neutral-500">
                    No quedan pedidos en viaje. Cerrá la jornada para finalizar.
                  </p>
                </div>
              )}
            </>
          )}

          {selectedRuta.estado === "FINALIZADA" && (
            <div className="rounded-xl border border-neutral-200 bg-white p-6 text-center dark:border-neutral-800 dark:bg-neutral-900">
              <p className="text-base font-medium text-neutral-900 dark:text-neutral-100">
                Jornada finalizada
              </p>
            </div>
          )}

          {selectedRuta.estado === "EN_CURSO" && (
            <BottomActionBar
              disabled={!current || mutating}
              mutating={mutating}
              onEntregarTotal={async () => {
                if (!current) return;
                setMutating(true);
                setError(null);
                try {
                  const entregas = current.items.map((it) => ({
                    pedidoItemId: it.pedidoItemId,
                    cantidadEntregada: it.cantidadReservada,
                  }));
                  await apiPost<Pedido>(`/api/pedidos/${current.id}/entregas`, {
                    entregas,
                  } satisfies EntregaRequest);
                  await refresh();
                } catch (err) {
                  setError(err instanceof Error ? err.message : "Error inesperado");
                } finally {
                  setMutating(false);
                }
              }}
              onEntregarParcial={() => {
                if (!current) return;
                setError(null);
                setParcialOpen(true);
              }}
              onReagendar={async () => {
                if (!current) return;
                setMutating(true);
                setError(null);
                try {
                  await apiPost<Pedido>(`/api/pedidos/${current.id}/reagendar`);
                  await refresh();
                } catch (err) {
                  setError(err instanceof Error ? err.message : "Error inesperado");
                } finally {
                  setMutating(false);
                }
              }}
              onRechazar={() => {
                if (!current) return;
                setError(null);
                setRechazarOpen(true);
              }}
              onCerrar={
                enViaje.length === 0
                  ? async () => {
                      if (!selectedRuta) return;
                      setMutating(true);
                      setError(null);
                      try {
                        await apiPost<Ruta>(`/api/rutas/${selectedRuta.id}/cerrar`);
                        await refresh();
                        setSuccess("Jornada cerrada");
                      } catch (err) {
                        setError(err instanceof Error ? err.message : "Error inesperado");
                      } finally {
                        setMutating(false);
                      }
                    }
                  : undefined
              }
            />
          )}
        </>
      ) : (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-neutral-200 bg-white p-8 text-center dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-neutral-500">No tenés rutas asignadas.</p>
        </div>
      )}

      {parcialOpen && current && (
        <ParcialSheet
          pedido={current}
          onCancel={() => setParcialOpen(false)}
          onRegister={async (cantidades) => {
            setMutating(true);
            setError(null);
            try {
              const entregas = current.items
                .filter((it) => (cantidades[it.pedidoItemId] ?? 0) > 0)
                .map((it) => ({
                  pedidoItemId: it.pedidoItemId,
                  cantidadEntregada: cantidades[it.pedidoItemId] ?? 0,
                }));
              await apiPost<Pedido>(`/api/pedidos/${current.id}/entregas`, {
                entregas,
              } satisfies EntregaRequest);
              await refresh();
            } catch (err) {
              setError(err instanceof Error ? err.message : "Error inesperado");
              throw err;
            } finally {
              setMutating(false);
            }
          }}
        />
      )}

      {rechazarOpen && current && (
        <ConfirmacionFrictionada
          title="Rechazar pedido"
          descripcion={`Confirmá el rechazo del pedido ${current.numero}. Esta acción no se puede deshacer.`}
          palabra="RECHAZAR"
          confirmLabel="Rechazar pedido"
          onConfirm={async () => {
            setMutating(true);
            setError(null);
            try {
              await apiPost<void>(`/api/pedidos/${current.id}/rechazar`);
              await refresh();
            } catch (err) {
              setError(err instanceof Error ? err.message : "Error inesperado");
              throw err;
            } finally {
              setMutating(false);
            }
          }}
          onClose={() => setRechazarOpen(false)}
        />
      )}
    </div>
  );
}

function RoutePicker({
  rutas,
  onSelect,
}: {
  rutas: Ruta[];
  onSelect: (id: number) => void;
}) {
  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
        Elegí tu ruta
      </h2>
      {rutas.length === 0 && (
        <p className="text-sm text-neutral-500">No tenés rutas asignadas.</p>
      )}
      {rutas.map((ruta) => (
        <button
          key={ruta.id}
          type="button"
          onClick={() => onSelect(ruta.id)}
          className="flex h-16 items-center justify-between rounded-xl border border-neutral-200 bg-white px-5 py-4 text-left transition-colors hover:bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900 dark:hover:bg-neutral-800/50"
        >
          <span className="font-medium text-neutral-900 dark:text-neutral-100">
            Ruta #{ruta.id}
          </span>
          <span className="text-sm text-neutral-500">{formatDate(ruta.fechaJornada)}</span>
        </button>
      ))}
    </div>
  );
}

function TurnoHeader({
  ruta,
  zonaNombre,
  entregados,
  enRuta,
  total,
}: {
  ruta: Ruta;
  zonaNombre: string | undefined;
  entregados: number;
  enRuta: number;
  total: number;
}) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex items-center justify-between gap-3">
        <div className="flex flex-col">
          <span className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
            Ruta #{ruta.id}
          </span>
          <span className="text-sm text-neutral-500">
            {zonaNombre ?? `Zona #${ruta.zonaId}`} · {formatDate(ruta.fechaJornada)}
          </span>
        </div>
        <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
          {entregados} entregados / {enRuta} en ruta
        </span>
      </div>
      {total > 0 && (
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-neutral-200 dark:bg-neutral-700">
          <div
            className="h-full rounded-full bg-blue-600 dark:bg-blue-500"
            style={{ width: `${Math.min(100, (entregados / total) * 100)}%` }}
          />
        </div>
      )}
    </div>
  );
}

function PedidoCard({
  pedido,
  cliente,
  clienteNombre,
  posicion,
  total,
  paradasRestantes,
}: {
  pedido: Pedido;
  cliente: Cliente | undefined;
  clienteNombre: string | undefined;
  posicion: number;
  total: number;
  paradasRestantes: { numero: string; clienteNombre: string }[];
}) {
  const domicilio = cliente?.domicilio?.trim();
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col">
          <span className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
            {pedido.numero}
          </span>
          <span className="text-base text-neutral-600 dark:text-neutral-300">
            {clienteNombre ?? `Cliente #${pedido.clienteId}`}
          </span>
          {domicilio ? (
            <span className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
              📍 {domicilio}
            </span>
          ) : (
            <span className="mt-1 text-xs italic text-neutral-400 dark:text-neutral-500">
              Sin domicilio cargado
            </span>
          )}
        </div>
        <span className="shrink-0 rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700 dark:bg-blue-950/50 dark:text-blue-300">
          {posicion} / {total}
        </span>
      </div>

      {pedido.observaciones?.trim() ? (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 dark:border-amber-900/50 dark:bg-amber-950/30">
          <p className="text-xs font-semibold text-amber-700 dark:text-amber-400">Observaciones</p>
          <p className="text-sm text-amber-800 dark:text-amber-300">
            {pedido.observaciones.trim()}
          </p>
        </div>
      ) : null}

      <ul className="flex flex-col divide-y divide-neutral-100 dark:divide-neutral-800">
        {pedido.items.map((it) => (
          <li key={it.pedidoItemId} className="flex items-center justify-between gap-3 py-3">
            <div className="flex flex-col">
              <span className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
                Item #{it.itemId}
              </span>
              <span className="text-xs text-neutral-500">
                Reservado: {formatNumber(it.cantidadReservada)} · Entregado:{" "}
                {formatNumber(it.cantidadEntregada)}
              </span>
            </div>
            <span className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
              {formatNumber(it.cantidadReservada)} u
            </span>
          </li>
        ))}
      </ul>

      <div className="flex items-center justify-between border-t border-neutral-200 pt-4 dark:border-neutral-800">
        <span className="text-sm font-medium text-neutral-500">Total</span>
        <span className="text-xl font-bold text-neutral-900 dark:text-neutral-100">
          {formatMoney(pedido.total)}
        </span>
      </div>

      {paradasRestantes.length > 0 && (
        <div className="border-t border-neutral-200 pt-4 dark:border-neutral-800">
          <p className="mb-2 text-sm font-semibold text-neutral-700 dark:text-neutral-200">
            Próximas paradas ({paradasRestantes.length})
          </p>
          <ol className="flex flex-col gap-1.5">
            {paradasRestantes.map((p, i) => (
              <li key={p.numero} className="flex items-center justify-between gap-3 text-sm">
                <span className="flex items-center gap-2 text-neutral-600 dark:text-neutral-300">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-neutral-100 text-[11px] font-semibold text-neutral-500 dark:bg-neutral-800 dark:text-neutral-400">
                    {posicion + i + 1}
                  </span>
                  <span className="truncate">{p.clienteNombre}</span>
                </span>
                <span className="shrink-0 font-medium text-neutral-500 dark:text-neutral-400">
                  {p.numero}
                </span>
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  );
}

function BottomActionBar({
  disabled,
  mutating,
  onEntregarTotal,
  onEntregarParcial,
  onReagendar,
  onRechazar,
  onCerrar,
}: {
  disabled: boolean;
  mutating: boolean;
  onEntregarTotal: () => void | Promise<void>;
  onEntregarParcial: () => void;
  onReagendar: () => void | Promise<void>;
  onRechazar: () => void;
  onCerrar?: () => void | Promise<void>;
}) {
  if (onCerrar) {
    return (
      <div className="fixed inset-x-0 bottom-0 border-t border-neutral-200 bg-white/95 backdrop-blur dark:border-neutral-800 dark:bg-neutral-950/95">
        <div className="mx-auto w-full max-w-md px-4 py-4 pb-[env(safe-area-inset-bottom)]">
          <Button
            className="h-16 w-full py-4 text-base"
            disabled={mutating}
            onClick={() => void onCerrar()}
          >
            {mutating ? "Cerrando..." : "Cerrar jornada"}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-x-0 bottom-0 border-t border-neutral-200 bg-white/95 backdrop-blur dark:border-neutral-800 dark:bg-neutral-950/95">
      <div className="mx-auto flex w-full max-w-md flex-col gap-3 px-4 py-4 pb-[env(safe-area-inset-bottom)]">
        <SwipeButton
          label="Deslizar para entregar total"
          onConfirm={onEntregarTotal}
          disabled={disabled || mutating}
        />
        <div className="grid grid-cols-3 gap-2">
          <Button
            variant="secondary"
            className="h-14 py-4 text-sm"
            disabled={disabled || mutating}
            onClick={onEntregarParcial}
          >
            Entrega parcial
          </Button>
          <Button
            variant="secondary"
            className="h-14 py-4 text-sm"
            disabled={disabled || mutating}
            onClick={() => void onReagendar()}
          >
            Re-agendar
          </Button>
          <Button
            variant="secondary"
            className="h-14 py-4 text-sm text-red-600 dark:text-red-400"
            disabled={disabled || mutating}
            onClick={onRechazar}
          >
            Rechazar
          </Button>
        </div>
      </div>
    </div>
  );
}

function ParcialSheet({
  pedido,
  onCancel,
  onRegister,
}: {
  pedido: Pedido;
  onCancel: () => void;
  onRegister: (cantidades: Record<number, number>) => Promise<void>;
}) {
  const [cantidades, setCantidades] = useState<Record<number, number>>(() =>
    Object.fromEntries(pedido.items.map((it) => [it.pedidoItemId, it.cantidadReservada]))
  );
  const [localError, setLocalError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function setCantidad(pedidoItemId: number, value: number) {
    setCantidades((prev) => ({ ...prev, [pedidoItemId]: value }));
  }

  function clamp(pedidoItemId: number, max: number, value: number) {
    return Math.max(0, Math.min(max, value));
  }

  async function handleRegister() {
    const total = Object.values(cantidades).reduce((sum, v) => sum + (v || 0), 0);
    if (total <= 0) {
      setLocalError("Registrá al menos una unidad para entregar.");
      return;
    }
    setLocalError(null);
    setSubmitting(true);
    try {
      await onRegister(cantidades);
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : "Error inesperado");
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-50">
      <div className="fixed inset-0 bg-black/40" aria-hidden="true" onClick={onCancel} />
      <div className="relative mx-auto w-full max-w-md rounded-t-2xl border-t border-neutral-200 bg-white p-5 pb-[env(safe-area-inset-bottom)] dark:border-neutral-800 dark:bg-neutral-900">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
            Entrega parcial · {pedido.numero}
          </h2>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Cerrar"
            className="rounded-md p-2 text-neutral-500 hover:bg-neutral-100 dark:hover:bg-neutral-800"
          >
            Cerrar
          </button>
        </div>

        <ul className="mb-4 flex max-h-72 flex-col gap-3 overflow-y-auto">
          {pedido.items.map((it) => {
            const value = cantidades[it.pedidoItemId] ?? 0;
            return (
              <li
                key={it.pedidoItemId}
                className="flex items-center justify-between gap-3 rounded-lg border border-neutral-200 p-3 dark:border-neutral-800"
              >
                <div className="flex min-w-0 flex-col">
                  <span className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">
                    Item #{it.itemId}
                  </span>
                  <span className="text-xs text-neutral-500">
                    Reservado: {formatNumber(it.cantidadReservada)}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setCantidad(it.pedidoItemId, clamp(it.pedidoItemId, it.cantidadReservada, value - 1))}
                    aria-label="Disminuir"
                    className="flex h-12 w-12 items-center justify-center rounded-md border border-neutral-300 text-xl font-semibold text-neutral-700 transition-colors hover:bg-neutral-100 disabled:opacity-40 dark:border-neutral-700 dark:text-neutral-200 dark:hover:bg-neutral-800"
                    disabled={value <= 0}
                  >
                    −
                  </button>
                  <span className="w-14 text-center text-lg font-bold text-neutral-900 dark:text-neutral-100">
                    {formatNumber(value)}
                  </span>
                  <button
                    type="button"
                    onClick={() => setCantidad(it.pedidoItemId, clamp(it.pedidoItemId, it.cantidadReservada, value + 1))}
                    aria-label="Aumentar"
                    className="flex h-12 w-12 items-center justify-center rounded-md border border-neutral-300 text-xl font-semibold text-neutral-700 transition-colors hover:bg-neutral-100 disabled:opacity-40 dark:border-neutral-700 dark:text-neutral-200 dark:hover:bg-neutral-800"
                    disabled={value >= it.cantidadReservada}
                  >
                    +
                  </button>
                </div>
              </li>
            );
          })}
        </ul>

        {localError && <div className="mb-3"><ErrorBox message={localError} /></div>}

        <div className="flex gap-2">
          <Button
            variant="secondary"
            className="h-14 flex-1 py-4 text-base"
            onClick={onCancel}
            disabled={submitting}
          >
            Cancelar
          </Button>
          <Button
            className="h-14 flex-1 py-4 text-base"
            onClick={() => void handleRegister()}
            disabled={submitting}
          >
            {submitting ? "Registrando..." : "Registrar parcial"}
          </Button>
        </div>
      </div>
    </div>
  );
}
