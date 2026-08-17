"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type {
  Cliente,
  Cobranza,
  EntregaRequest,
  FormaPago,
  PageResponse,
  Pedido,
  Remito,
  Ruta,
  Zona,
} from "@/lib/types";
import { formatDate, formatMoney, formatNumber } from "@/lib/format";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import ConfirmacionFrictionada from "@/components/ConfirmacionFrictionada";
import SwipeButton from "@/components/SwipeButton";
import SustitucionModal from "@/components/SustitucionModal";

const ALLOWED_ROLES = ["REPARTIDOR", "ADMINISTRATIVO"];

const FORMAS_PAGO: FormaPago[] = ["EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO"];

const FORMA_PAGO_LABELS: Record<string, string> = {
  EFECTIVO: "Efectivo",
  TRANSFERENCIA: "Transferencia",
  TARJETA: "Tarjeta",
  OTRO: "Otro",
};

type Fase = "sinRuta" | "antesDeSalir" | "enViaje" | "rendicion" | "finalizada";

type Accion =
  | { tipo: "parcial"; pedidoId: number }
  | { tipo: "cobrar"; pedidoId: number }
  | { tipo: "sustituir"; pedidoId: number }
  | { tipo: "rechazar"; pedidoId: number };

export default function TurnoPage() {
  const { user } = useAuth();

  const canView = user ? user.roles.some((r) => ALLOWED_ROLES.includes(r)) : false;

  const [rutas, setRutas] = useState<Ruta[]>([]);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [zonas, setZonas] = useState<Zona[]>([]);
  const [cobradoPorPedido, setCobradoPorPedido] = useState<Map<number, number>>(new Map());
  const [remitoPedidoIds, setRemitoPedidoIds] = useState<Set<number>>(new Set());

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [selectedRutaId, setSelectedRutaId] = useState<number | null>(null);
  const didAutoSelect = useRef(false);

  const [mutating, setMutating] = useState(false);
  const [accion, setAccion] = useState<Accion | null>(null);
  const [alertaAdicionalDismissed, setAlertaAdicionalDismissed] = useState(false);
  const [adicionales, setAdicionales] = useState<Pedido[]>([]);

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

  const loadRoutePedidos = useCallback(async (ids: number[]) => {
    if (ids.length === 0) {
      setPedidos([]);
      return;
    }
    const data = await apiGet<PageResponse<Pedido>>(`/api/pedidos?ids=${ids.join(",")}`);
    setPedidos(data.content);
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

  const loadCobranzas = useCallback(async (pedidosRuta: Pedido[]) => {
    const clienteIds = [...new Set(pedidosRuta.map((p) => p.clienteId))];
    const map = new Map<number, number>();
    await Promise.all(
      clienteIds.map(async (cid) => {
        try {
          const list = await apiGet<Cobranza[]>(`/api/cobranzas?clienteId=${cid}`);
          for (const c of list) {
            if (c.pedidoId == null) continue;
            map.set(c.pedidoId, (map.get(c.pedidoId) ?? 0) + c.monto);
          }
        } catch {
          // El saldo es una ayuda opcional; si falla, se muestra el total
        }
      })
    );
    setCobradoPorPedido(map);
  }, []);

  const loadRemitos = useCallback(async (pedidosRuta: Pedido[]) => {
    const withRemito = new Set<number>();
    await Promise.all(
      pedidosRuta.map(async (p) => {
        try {
          const list = await apiGet<Remito[]>(`/api/remitos?pedidoId=${p.id}`);
          if (list.length > 0) withRemito.add(p.id);
        } catch {
          // Remitos son informativos; si falla, se omiten
        }
      })
    );
    setRemitoPedidoIds(withRemito);
  }, []);

  const refresh = useCallback(async () => {
    setError(null);
    setSuccess(null);
    setAccion(null);
    setAlertaAdicionalDismissed(false);
    await loadRutas();
  }, [loadRutas]);

  useEffect(() => {
    if (repartidorId == null) return;
    let active = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        await Promise.all([loadRutas(), loadClientes(), loadZonas()]);
      } catch {
        // los loaders ya capturan el error
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [repartidorId, loadRutas, loadClientes, loadZonas]);

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

  useEffect(() => {
    if (!selectedRuta) {
      setPedidos([]);
      return;
    }
    void loadRoutePedidos(selectedRuta.pedidoIds);
  }, [selectedRuta, loadRoutePedidos]);

  const routePedidos = useMemo(() => {
    if (!selectedRuta) return [] as Pedido[];
    return selectedRuta.pedidoIds
      .map((id) => pedidosById.get(id))
      .filter((p): p is Pedido => p !== undefined);
  }, [selectedRuta, pedidosById]);

  const fase = useMemo<Fase>(() => {
    if (!selectedRuta) return "sinRuta";
    if (selectedRuta.estado === "PLANIFICADA") return "antesDeSalir";
    if (selectedRuta.estado === "FINALIZADA") return "finalizada";
    const hayEnViaje = routePedidos.some((p) => p.estado === "EN_VIAJE");
    return hayEnViaje ? "enViaje" : "rendicion";
  }, [selectedRuta, routePedidos]);

  useEffect(() => {
    if (routePedidos.length === 0) {
      setCobradoPorPedido(new Map());
      return;
    }
    void loadCobranzas(routePedidos);
  }, [routePedidos, loadCobranzas]);

  useEffect(() => {
    if (fase !== "rendicion") return;
    const entregados = routePedidos.filter(
      (p) => p.estado === "ENTREGADO" || p.estado === "ENTREGADO_PARCIAL"
    );
    if (entregados.length === 0) return;
    void loadRemitos(entregados);
  }, [fase, routePedidos, loadRemitos]);

  const enViaje = useMemo(() => routePedidos.filter((p) => p.estado === "EN_VIAJE"), [routePedidos]);

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
  const enViajeClienteKey = useMemo(
    () => [...enViajeClienteIds].sort().join(","),
    [enViajeClienteIds]
  );

  useEffect(() => {
    if (enViajeClienteIds.size === 0) {
      setAdicionales([]);
      return;
    }
    let active = true;
    (async () => {
      const acumulados: Pedido[] = [];
      for (const clienteId of enViajeClienteIds) {
        try {
          const data = await apiGet<PageResponse<Pedido>>(
            `/api/pedidos?clienteId=${clienteId}&size=100`
          );
          acumulados.push(...data.content);
        } catch {
          // Los pedidos adicionales son una alerta complementaria
        }
      }
      if (active) setAdicionales(acumulados);
    })();
    return () => {
      active = false;
    };
  }, [enViajeClienteKey]); // eslint-disable-line react-hooks/exhaustive-deps

  const pedidosAdicionales = useMemo(
    () => adicionales.filter((p) => p.estado === "PENDIENTE_CONFIRMACION"),
    [adicionales]
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

  const zonaNombre = selectedRuta ? zonasById.get(selectedRuta.zonaId) : undefined;

  const accionPedido = useMemo(
    () => (accion ? (pedidosById.get(accion.pedidoId) ?? null) : null),
    [accion, pedidosById]
  );

  async function entregarTotal(pedido: Pedido) {
    setMutating(true);
    setError(null);
    try {
      const entregas = pedido.items.map((it) => ({
        pedidoItemId: it.pedidoItemId,
        cantidadEntregada: it.cantidadReservada,
      }));
      await apiPost<Pedido>(`/api/pedidos/${pedido.id}/entregas`, {
        entregas,
      } satisfies EntregaRequest);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setMutating(false);
    }
  }

  async function reagendar(pedido: Pedido) {
    setMutating(true);
    setError(null);
    try {
      await apiPost<Pedido>(`/api/pedidos/${pedido.id}/reagendar`);
      await refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setMutating(false);
    }
  }

  async function cerrarJornada() {
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

  if (!canView) {
    return (
      <div className="mx-auto flex min-h-[60vh] w-full max-w-md flex-col items-center justify-center gap-4 px-4">
        <h1 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
          Mi Jornada
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
    <div className="mx-auto flex w-full max-w-md flex-col gap-4 px-4 pb-40 pt-4 sm:pt-8">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">Mi Jornada</h1>
        <p className="text-sm text-neutral-500">Flujo de entrega del día, paso a paso.</p>
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
        <Loading label="Cargando tu jornada..." />
      ) : fase === "sinRuta" || !selectedRuta ? (
        <SinRuta />
      ) : (
        <>
          <WizardSteps fase={fase} />
          <TurnoHeader
            ruta={selectedRuta}
            zonaNombre={zonaNombre}
            entregados={entregadosCount}
            total={routePedidos.length}
          />

          {fase === "antesDeSalir" && (
            <AntesDeSalir
              ruta={selectedRuta}
              repartidorEmail={user?.email}
              pedidos={routePedidos}
              mutating={mutating}
              onIniciar={async () => {
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
            />
          )}

          {fase === "enViaje" && (
            <EnViaje
              pedidos={routePedidos}
              clientesById={clientesById}
              cobradoPorPedido={cobradoPorPedido}
              pedidosAdicionalesPorCliente={
                alertaAdicionalDismissed
                  ? new Map()
                  : pedidosAdicionalesPorCliente
              }
              onDescartarAdicionales={() => setAlertaAdicionalDismissed(true)}
              onEntregarTotal={entregarTotal}
              onReagendar={reagendar}
              onAccion={setAccion}
              mutating={mutating}
            />
          )}

          {fase === "rendicion" && (
            <Rendicion
              ruta={selectedRuta}
              pedidos={routePedidos}
              cobradoPorPedido={cobradoPorPedido}
              remitoPedidoIds={remitoPedidoIds}
              mutating={mutating}
              onCerrar={cerrarJornada}
              onCobrar={(id) => setAccion({ tipo: "cobrar", pedidoId: id })}
            />
          )}

          {fase === "finalizada" && <Finalizada ruta={selectedRuta} />}
        </>
      )}

      {accion && accionPedido && accion.tipo === "parcial" && (
        <ParcialSheet
          pedido={accionPedido}
          onCancel={() => setAccion(null)}
          onRegister={async (cantidades) => {
            setMutating(true);
            setError(null);
            try {
              const entregas = accionPedido.items
                .filter((it) => (cantidades[it.pedidoItemId] ?? 0) > 0)
                .map((it) => ({
                  pedidoItemId: it.pedidoItemId,
                  cantidadEntregada: cantidades[it.pedidoItemId] ?? 0,
                }));
              await apiPost<Pedido>(`/api/pedidos/${accionPedido.id}/entregas`, {
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

      {accion && accionPedido && accion.tipo === "cobrar" && (
        <CobranzaSheet
          pedido={accionPedido}
          cobrado={cobradoPorPedido.get(accionPedido.id) ?? 0}
          onCancel={() => setAccion(null)}
          onRegister={async (monto, formaPago) => {
            setMutating(true);
            setError(null);
            try {
              await apiPost<Cobranza>("/api/cobranzas", {
                clienteId: accionPedido.clienteId,
                pedidoId: accionPedido.id,
                monto,
                formaPago,
              });
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

      {accion && accionPedido && accion.tipo === "sustituir" && (
        <SustitucionModal
          pedido={accionPedido}
          onClose={() => setAccion(null)}
          onConfirm={async () => {
            setAccion(null);
            await refresh();
          }}
        />
      )}

      {accion && accionPedido && accion.tipo === "rechazar" && (
        <ConfirmacionFrictionada
          title="Rechazar pedido"
          descripcion={`Confirmá el rechazo del pedido ${accionPedido.numero}. Esta acción no se puede deshacer.`}
          palabra="RECHAZAR"
          confirmLabel="Rechazar pedido"
          onConfirm={async () => {
            setMutating(true);
            setError(null);
            try {
              await apiPost<void>(`/api/pedidos/${accionPedido.id}/rechazar`);
              await refresh();
            } catch (err) {
              setError(err instanceof Error ? err.message : "Error inesperado");
              throw err;
            } finally {
              setMutating(false);
            }
          }}
          onClose={() => setAccion(null)}
        />
      )}
    </div>
  );
}

function SinRuta() {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-neutral-200 bg-white p-8 text-center dark:border-neutral-800 dark:bg-neutral-900">
      <p className="text-base font-medium text-neutral-900 dark:text-neutral-100">
        No tenés una ruta asignada hoy
      </p>
      <p className="text-sm text-neutral-500">
        Cuando el encargado te asigne una ruta, la vas a ver acá para iniciar tu jornada.
      </p>
      <Link href="/" className="text-sm font-medium text-blue-600 hover:underline dark:text-blue-400">
        Volver al panel
      </Link>
    </div>
  );
}

function WizardSteps({ fase }: { fase: Fase }) {
  const pasos: { key: Fase; label: string }[] = [
    { key: "antesDeSalir", label: "Antes de salir" },
    { key: "enViaje", label: "En viaje" },
    { key: "rendicion", label: "Rendición" },
  ];
  const activo = fase === "antesDeSalir" ? 0 : fase === "enViaje" ? 1 : 2;
  return (
    <ol className="flex items-center gap-1">
      {pasos.map((paso, i) => {
        const completado = i < activo;
        const esActivo = i === activo;
        return (
          <li key={paso.key} className="flex flex-1 items-center gap-1">
            <div
              className={`flex flex-1 items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-semibold ${
                esActivo
                  ? "bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300"
                  : completado
                    ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300"
                    : "bg-neutral-100 text-neutral-400 dark:bg-neutral-800 dark:text-neutral-500"
              }`}
            >
              <span>{completado ? "✓" : `${i + 1}.`}</span>
              <span className="truncate">{paso.label}</span>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

function TurnoHeader({
  ruta,
  zonaNombre,
  entregados,
  total,
}: {
  ruta: Ruta;
  zonaNombre: string | undefined;
  entregados: number;
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
          {entregados}/{total} entregados
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

function AntesDeSalir({
  ruta,
  repartidorEmail,
  pedidos,
  mutating,
  onIniciar,
}: {
  ruta: Ruta;
  repartidorEmail: string | undefined;
  pedidos: Pedido[];
  mutating: boolean;
  onIniciar: () => Promise<void>;
}) {
  const totalBultos = pedidos.reduce(
    (sum, p) => sum + p.items.reduce((s, it) => s + it.cantidadReservada, 0),
    0
  );
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
          Tu ruta asignada
        </h2>
        <p className="text-sm text-neutral-500">
          Revisá el detalle y, cuando estés listo, iniciá la jornada.
        </p>
      </div>

      <dl className="flex flex-col divide-y divide-neutral-100 dark:divide-neutral-800">
        <div className="flex items-center justify-between gap-3 py-2.5">
          <dt className="text-sm text-neutral-500">Repartidor</dt>
          <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
            {repartidorEmail ?? `Repartidor #${ruta.repartidorId}`}
          </dd>
        </div>
        <div className="flex items-center justify-between gap-3 py-2.5">
          <dt className="text-sm text-neutral-500">Fecha de jornada</dt>
          <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
            {formatDate(ruta.fechaJornada)}
          </dd>
        </div>
        <div className="flex items-center justify-between gap-3 py-2.5">
          <dt className="text-sm text-neutral-500">Pedidos</dt>
          <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
            {pedidos.length}
          </dd>
        </div>
        <div className="flex items-center justify-between gap-3 py-2.5">
          <dt className="text-sm text-neutral-500">Bultos a transportar</dt>
          <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
            {formatNumber(totalBultos)} / {formatNumber(ruta.capacidadBultos)}
          </dd>
        </div>
      </dl>

      <SwipeButton label="Deslizar para iniciar jornada" onConfirm={onIniciar} disabled={mutating} />
      <Button className="h-14 py-4 text-base" disabled={mutating} onClick={() => void onIniciar()}>
        {mutating ? "Iniciando..." : "Iniciar Jornada"}
      </Button>
    </div>
  );
}

function EnViaje({
  pedidos,
  clientesById,
  cobradoPorPedido,
  pedidosAdicionalesPorCliente,
  onDescartarAdicionales,
  onEntregarTotal,
  onReagendar,
  onAccion,
  mutating,
}: {
  pedidos: Pedido[];
  clientesById: Map<number, Cliente>;
  cobradoPorPedido: Map<number, number>;
  pedidosAdicionalesPorCliente: Map<number, Pedido[]>;
  onDescartarAdicionales: () => void;
  onEntregarTotal: (pedido: Pedido) => Promise<void>;
  onReagendar: (pedido: Pedido) => Promise<void>;
  onAccion: (accion: Accion) => void;
  mutating: boolean;
}) {
  return (
    <div className="flex flex-col gap-4">
      {pedidosAdicionalesPorCliente.size > 0 && (
        <div className="flex flex-col gap-2 rounded-xl border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-950/40">
          <div className="flex items-start justify-between gap-3">
            <div className="flex flex-col gap-1.5">
              {[...pedidosAdicionalesPorCliente.entries()].map(([clienteId, adicionales]) => (
                <p key={clienteId} className="text-sm text-blue-800 dark:text-blue-300">
                  {clientesById.get(clienteId)?.razonSocial ?? `Cliente #${clienteId}`} tiene un
                  pedido adicional cargado hoy:{" "}
                  {adicionales.map((p) => p.numero).join(", ")}
                </p>
              ))}
              <p className="text-xs text-blue-600 dark:text-blue-400">
                No es entregable en esta ruta; coordiná el envío por separado.
              </p>
            </div>
            <button
              type="button"
              onClick={onDescartarAdicionales}
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

      <div className="flex flex-col gap-3">
        {pedidos.map((pedido, i) => (
          <StopCard
            key={pedido.id}
            pedido={pedido}
            cliente={clientesById.get(pedido.clienteId)}
            posicion={i + 1}
            total={pedidos.length}
            cobrado={cobradoPorPedido.get(pedido.id) ?? 0}
            mutating={mutating}
            onEntregarTotal={() => onEntregarTotal(pedido)}
            onReagendar={() => onReagendar(pedido)}
            onAccion={onAccion}
          />
        ))}
      </div>
    </div>
  );
}

function StopCard({
  pedido,
  cliente,
  posicion,
  total,
  cobrado,
  mutating,
  onEntregarTotal,
  onReagendar,
  onAccion,
}: {
  pedido: Pedido;
  cliente: Cliente | undefined;
  posicion: number;
  total: number;
  cobrado: number;
  mutating: boolean;
  onEntregarTotal: () => Promise<void>;
  onReagendar: () => Promise<void>;
  onAccion: (accion: Accion) => void;
}) {
  const domicilio = cliente?.domicilio?.trim();
  const esEnViaje = pedido.estado === "EN_VIAJE";
  const saldo = Math.max(0, pedido.total - cobrado);
  const cobradoTotal = cobrado > 0;
  const puedeCobrar = esEnViaje || pedido.estado === "ENTREGADO" || pedido.estado === "ENTREGADO_PARCIAL";

  return (
    <article className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col">
          <span className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
            {pedido.numero}
          </span>
          <span className="text-base text-neutral-600 dark:text-neutral-300">
            {cliente?.razonSocial ?? `Cliente #${pedido.clienteId}`}
          </span>
          {cliente?.telefono?.trim() && (
            <span className="mt-0.5 text-sm text-neutral-500 dark:text-neutral-400">
              📞 {cliente.telefono.trim()}
            </span>
          )}
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
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700 dark:bg-blue-950/50 dark:text-blue-300">
            {posicion} / {total}
          </span>
          <EstadoMini estado={pedido.estado} />
        </div>
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
                Entregado: {formatNumber(it.cantidadEntregada)} de{" "}
                {formatNumber(it.cantidadReservada)}
              </span>
            </div>
            <span className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
              {formatNumber(it.cantidadReservada)} u
            </span>
          </li>
        ))}
      </ul>

      <div className="flex flex-col gap-1 border-t border-neutral-200 pt-3 dark:border-neutral-800">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-neutral-500">Total a cobrar</span>
          <span className="text-xl font-bold text-neutral-900 dark:text-neutral-100">
            {formatMoney(pedido.total)}
          </span>
        </div>
        {cobradoTotal && (
          <div className="flex items-center justify-between text-sm">
            <span className="text-neutral-500">Cobrado</span>
            <span className="font-medium text-emerald-600 dark:text-emerald-400">
              {formatMoney(cobrado)}
            </span>
          </div>
        )}
        {cobradoTotal && saldo > 0 && (
          <div className="flex items-center justify-between text-sm">
            <span className="text-neutral-500">Saldo</span>
            <span className="font-semibold text-amber-600 dark:text-amber-400">
              {formatMoney(saldo)}
            </span>
          </div>
        )}
      </div>

      {esEnViaje ? (
        <div className="flex flex-col gap-3">
          <SwipeButton
            label="Deslizar para entregar total"
            onConfirm={onEntregarTotal}
            disabled={mutating}
          />
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="secondary"
              className="h-12 py-2 text-sm"
              disabled={mutating}
              onClick={() => onAccion({ tipo: "parcial", pedidoId: pedido.id })}
            >
              Entrega parcial
            </Button>
            <Button
              variant="secondary"
              className="h-12 py-2 text-sm"
              disabled={mutating}
              onClick={() => onAccion({ tipo: "cobrar", pedidoId: pedido.id })}
            >
              Cobrar
            </Button>
            <Button
              variant="secondary"
              className="h-12 py-2 text-sm"
              disabled={mutating}
              onClick={() => onAccion({ tipo: "sustituir", pedidoId: pedido.id })}
            >
              Sustituir
            </Button>
            <Button
              variant="secondary"
              className="h-12 py-2 text-sm"
              disabled={mutating}
              onClick={() => void onReagendar()}
            >
              Re-agendar
            </Button>
          </div>
          <Button
            variant="secondary"
            className="h-12 w-full py-2 text-sm text-red-600 dark:text-red-400"
            disabled={mutating}
            onClick={() => onAccion({ tipo: "rechazar", pedidoId: pedido.id })}
          >
            Rechazar
          </Button>
        </div>
      ) : (
        puedeCobrar && (
          <div className="flex flex-col gap-2">
            <Button
              variant="secondary"
              className="h-12 w-full py-2 text-sm"
              disabled={mutating || saldo <= 0}
              onClick={() => onAccion({ tipo: "cobrar", pedidoId: pedido.id })}
            >
              {cobradoTotal ? "Asentar cobro pendiente" : "Cobrar"}
            </Button>
            <Button
              variant="secondary"
              className="h-12 w-full py-2 text-sm"
              disabled={mutating}
              onClick={() => onAccion({ tipo: "sustituir", pedidoId: pedido.id })}
            >
              Sustituir
            </Button>
          </div>
        )
      )}
    </article>
  );
}

function EstadoMini({ estado }: { estado: Pedido["estado"] }) {
  const map: Record<string, { cls: string; label: string }> = {
    EN_VIAJE: { cls: "bg-blue-100 text-blue-700 dark:bg-blue-950/50 dark:text-blue-300", label: "En viaje" },
    ENTREGADO: { cls: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300", label: "Entregado" },
    ENTREGADO_PARCIAL: { cls: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300", label: "Parcial" },
    RE_AGENDADO: { cls: "bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300", label: "Re-agendado" },
    RECHAZADO: { cls: "bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300", label: "Rechazado" },
  };
  const c = map[estado] ?? { cls: "bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300", label: estado };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ${c.cls}`}>
      {c.label}
    </span>
  );
}

function Rendicion({
  ruta,
  pedidos,
  cobradoPorPedido,
  remitoPedidoIds,
  mutating,
  onCerrar,
  onCobrar,
}: {
  ruta: Ruta;
  pedidos: Pedido[];
  cobradoPorPedido: Map<number, number>;
  remitoPedidoIds: Set<number>;
  mutating: boolean;
  onCerrar: () => Promise<void>;
  onCobrar: (pedidoId: number) => void;
}) {
  const entregados = pedidos.filter(
    (p) => p.estado === "ENTREGADO" || p.estado === "ENTREGADO_PARCIAL"
  );
  const cobradoTotal = [...cobradoPorPedido.values()].reduce((a, b) => a + b, 0);
  const cobranzasAsentadas = entregados.filter((p) => (cobradoPorPedido.get(p.id) ?? 0) > 0);
  const remitos = entregados.filter((p) => remitoPedidoIds.has(p.id));
  const sinCobrar = entregados.filter((p) => (cobradoPorPedido.get(p.id) ?? 0) <= 0);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-4 rounded-xl border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex flex-col gap-1">
          <h2 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
            ¡Ruta completa!
          </h2>
          <p className="text-sm text-neutral-500">
            Todas las paradas fueron entregadas. Revisá el resumen y cerrá la jornada.
          </p>
        </div>

        <dl className="flex flex-col divide-y divide-neutral-100 dark:divide-neutral-800">
          <div className="flex items-center justify-between gap-3 py-2.5">
            <dt className="text-sm text-neutral-500">Pedidos entregados</dt>
            <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
              {entregados.length} / {pedidos.length}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-3 py-2.5">
            <dt className="text-sm text-neutral-500">Cobranzas asentadas</dt>
            <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
              {cobranzasAsentadas.length} · {formatMoney(cobradoTotal)}
            </dd>
          </div>
          <div className="flex items-center justify-between gap-3 py-2.5">
            <dt className="text-sm text-neutral-500">Remitos</dt>
            <dd className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
              {remitos.length}
            </dd>
          </div>
        </dl>
      </div>

      {sinCobrar.length > 0 && (
        <div className="flex flex-col gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-900/50 dark:bg-amber-950/30">
          <p className="text-sm font-semibold text-amber-800 dark:text-amber-300">
            Tenés {sinCobrar.length} pedido{sinCobrar.length === 1 ? "" : "s"} sin cobrar
          </p>
          <div className="flex flex-col gap-2">
            {sinCobrar.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => onCobrar(p.id)}
                className="flex items-center justify-between rounded-lg border border-amber-300 bg-white px-3 py-2 text-left text-sm dark:border-amber-800 dark:bg-neutral-900"
              >
                <span className="font-medium text-neutral-900 dark:text-neutral-100">{p.numero}</span>
                <span className="font-semibold text-amber-700 dark:text-amber-400">
                  {formatMoney(p.total)}
                </span>
              </button>
            ))}
          </div>
          <p className="text-xs text-amber-600 dark:text-amber-400">
            Podés asentar cobranzas pendientes antes de cerrar la jornada.
          </p>
        </div>
      )}

      <SwipeButton label="Deslizar para cerrar jornada" onConfirm={onCerrar} disabled={mutating} />
      <Button className="h-14 w-full py-4 text-base" disabled={mutating} onClick={() => void onCerrar()}>
        {mutating ? "Cerrando..." : "Cerrar Jornada"}
      </Button>

      <p className="text-xs text-neutral-400">
        Ruta #{ruta.id} · {formatDate(ruta.fechaJornada)}
      </p>
    </div>
  );
}

function Finalizada({ ruta }: { ruta: Ruta }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-neutral-200 bg-white p-8 text-center dark:border-neutral-800 dark:bg-neutral-900">
      <p className="text-base font-medium text-emerald-600 dark:text-emerald-400">✓</p>
      <p className="text-base font-medium text-neutral-900 dark:text-neutral-100">
        Jornada finalizada
      </p>
      <p className="text-sm text-neutral-500">
        La ruta #{ruta.id} del {formatDate(ruta.fechaJornada)} quedó cerrada.
      </p>
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

function CobranzaSheet({
  pedido,
  cobrado,
  onCancel,
  onRegister,
}: {
  pedido: Pedido;
  cobrado: number;
  onCancel: () => void;
  onRegister: (monto: number, formaPago: FormaPago) => Promise<void>;
}) {
  const saldo = Math.max(0, pedido.total - cobrado);
  const [monto, setMonto] = useState<string>(saldo > 0 ? String(saldo) : "");
  const [formaPago, setFormaPago] = useState<FormaPago>("EFECTIVO");
  const [localError, setLocalError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleRegister() {
    const montoNum = Number(monto);
    if (!monto || !Number.isFinite(montoNum) || montoNum <= 0) {
      setLocalError("Ingresá un monto mayor a cero.");
      return;
    }
    setLocalError(null);
    setSubmitting(true);
    try {
      await onRegister(montoNum, formaPago);
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
            Cobrar · {pedido.numero}
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

        <div className="mb-4 flex flex-col gap-1 rounded-lg border border-neutral-200 p-3 text-sm dark:border-neutral-800">
          <div className="flex items-center justify-between">
            <span className="text-neutral-500">Total pedido</span>
            <span className="font-semibold text-neutral-900 dark:text-neutral-100">
              {formatMoney(pedido.total)}
            </span>
          </div>
          {cobrado > 0 && (
            <div className="flex items-center justify-between">
              <span className="text-neutral-500">Ya cobrado</span>
              <span className="font-medium text-emerald-600 dark:text-emerald-400">
                {formatMoney(cobrado)}
              </span>
            </div>
          )}
          <div className="flex items-center justify-between">
            <span className="text-neutral-500">Saldo a cobrar</span>
            <span className="font-semibold text-amber-600 dark:text-amber-400">
              {formatMoney(saldo)}
            </span>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cobranza-monto" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Monto
            </label>
            <input
              id="cobranza-monto"
              type="number"
              min="0.01"
              step="0.01"
              value={monto}
              onChange={(e) => setMonto(e.target.value)}
              className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="cobranza-forma" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Forma de pago
            </label>
            <select
              id="cobranza-forma"
              value={formaPago}
              onChange={(e) => setFormaPago(e.target.value as FormaPago)}
              className="rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
            >
              {FORMAS_PAGO.map((f) => (
                <option key={f} value={f}>
                  {FORMA_PAGO_LABELS[f]}
                </option>
              ))}
            </select>
          </div>
        </div>

        {localError && <div className="mt-3"><ErrorBox message={localError} /></div>}

        <div className="mt-4 flex gap-2">
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
            {submitting ? "Registrando..." : "Registrar cobranza"}
          </Button>
        </div>
      </div>
    </div>
  );
}
