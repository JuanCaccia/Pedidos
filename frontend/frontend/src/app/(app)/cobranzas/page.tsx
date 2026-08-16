"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import type {
  Cliente,
  Cobranza,
  EstadoCuenta,
  FormaPago,
  PageResponse,
  Pedido,
  Remito,
  ResumenCaja,
} from "@/lib/types";
import { formatDate, formatDateTime, formatMoney, formatNumber } from "@/lib/format";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";
import Modal from "@/components/Modal";
import Combobox from "@/components/Combobox";
import { exportarCSV } from "@/lib/export";

const INPUT_CLASS =
  "rounded-md border border-neutral-300 bg-white px-3 py-2 text-sm text-neutral-900 outline-none focus:border-blue-500 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100";

const FORMAS_PAGO: FormaPago[] = ["EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO"];

const FORMA_PAGO_LABELS: Record<string, string> = {
  EFECTIVO: "Efectivo",
  TRANSFERENCIA: "Transferencia",
  TARJETA: "Tarjeta",
  OTRO: "Otro",
};

function todayIso(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export default function CobranzasPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<string>("");

  const [pedidos, setPedidos] = useState<Pedido[]>([]);

  const [cuenta, setCuenta] = useState<EstadoCuenta | null>(null);
  const [cuentaLoading, setCuentaLoading] = useState(false);
  const [cuentaError, setCuentaError] = useState<string | null>(null);

  const [cobranzas, setCobranzas] = useState<Cobranza[]>([]);
  const [cobranzasLoading, setCobranzasLoading] = useState(true);
  const [cobranzasError, setCobranzasError] = useState<string | null>(null);

  const [remitos, setRemitos] = useState<Remito[]>([]);
  const [remitosLoading, setRemitosLoading] = useState(true);
  const [remitosError, setRemitosError] = useState<string | null>(null);

  const [expandedRemitoId, setExpandedRemitoId] = useState<number | null>(null);

  const [success, setSuccess] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [resumenDesde, setResumenDesde] = useState(todayIso);
  const [resumenHasta, setResumenHasta] = useState(todayIso);
  const [resumen, setResumen] = useState<ResumenCaja | null>(null);
  const [resumenLoading, setResumenLoading] = useState(true);
  const [resumenError, setResumenError] = useState<string | null>(null);

  const loadResumen = useCallback(async (desde: string, hasta: string) => {
    setResumenLoading(true);
    setResumenError(null);
    try {
      const params = new URLSearchParams();
      if (desde) params.set("desde", desde);
      if (hasta) params.set("hasta", hasta);
      const query = params.toString();
      setResumen(await apiGet<ResumenCaja>(`/api/reportes/caja${query ? `?${query}` : ""}`));
    } catch (err) {
      setResumenError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setResumenLoading(false);
    }
  }, []);

  const loadClientes = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Cliente>>("/api/clientes?size=500");
      setClientes(data.content);
    } catch {
      // El select quedará vacío; el error aparecerá al intentar usarlo
    }
  }, []);

  const loadPedidos = useCallback(async () => {
    try {
      const data = await apiGet<PageResponse<Pedido>>("/api/pedidos?size=500");
      setPedidos(data.content);
    } catch {
      // Las tablas mostrarán el id numérico de pedido al fallar el join
    }
  }, []);

  const loadCuenta = useCallback(async (id: number) => {
    setCuentaLoading(true);
    setCuentaError(null);
    try {
      setCuenta(await apiGet<EstadoCuenta>(`/api/cobranzas/clientes/${id}/cuenta`));
    } catch (err) {
      setCuenta(null);
      setCuentaError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setCuentaLoading(false);
    }
  }, []);

  const loadCobranzas = useCallback(async (id: string) => {
    setCobranzasLoading(true);
    setCobranzasError(null);
    try {
      const url = id ? `/api/cobranzas?clienteId=${id}` : "/api/cobranzas";
      setCobranzas(await apiGet<Cobranza[]>(url));
    } catch (err) {
      setCobranzasError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setCobranzasLoading(false);
    }
  }, []);

  const loadRemitos = useCallback(async (id: string) => {
    setRemitosLoading(true);
    setRemitosError(null);
    try {
      const url = id ? `/api/remitos?clienteId=${id}` : "/api/remitos";
      setRemitos(await apiGet<Remito[]>(url));
    } catch (err) {
      setRemitosError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setRemitosLoading(false);
    }
  }, []);

  useEffect(() => {
    loadClientes();
  }, [loadClientes]);

  useEffect(() => {
    loadPedidos();
  }, [loadPedidos]);

  useEffect(() => {
    loadCobranzas(clienteId);
    loadRemitos(clienteId);
  }, [loadCobranzas, loadRemitos, clienteId]);

  useEffect(() => {
    loadResumen(todayIso(), todayIso());
  }, [loadResumen]);

  const clientesById = useMemo(() => new Map(clientes.map((c) => [c.id, c.razonSocial])), [clientes]);

  const pedidosById = useMemo(() => new Map(pedidos.map((p) => [p.id, p])), [pedidos]);

  function handleClienteChange(value: number | null) {
    setClienteId(value != null ? String(value) : "");
    setCuenta(null);
    setSuccess(null);
    if (value != null) {
      loadCuenta(value);
    }
  }

  async function handleCobranzaCreated() {
    setShowForm(false);
    setSuccess("Cobranza registrada correctamente.");
    await Promise.all([loadCobranzas(clienteId), loadRemitos(clienteId)]);
    if (clienteId) await loadCuenta(Number(clienteId));
  }

  async function exportarCaja() {
    try {
      const params = new URLSearchParams();
      if (resumenDesde) params.set("desde", resumenDesde);
      if (resumenHasta) params.set("hasta", resumenHasta);
      const query = params.toString();
      await exportarCSV(
        `/api/reportes/caja/exportar.csv${query ? `?${query}` : ""}`,
        "caja.csv"
      );
    } catch (err) {
      setResumenError(err instanceof Error ? err.message : "Error inesperado");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Cobranzas</h1>
        <Button
          onClick={() => {
            setFormError(null);
            setShowForm(true);
          }}
        >
          Registrar cobranza
        </Button>
      </div>

      {success && (
        <div className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300">
          {success}
        </div>
      )}

      <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Resumen de caja</h2>
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="resumen-desde" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
                Desde
              </label>
              <input
                id="resumen-desde"
                type="date"
                value={resumenDesde}
                onChange={(e) => setResumenDesde(e.target.value)}
                className={INPUT_CLASS}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label htmlFor="resumen-hasta" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
                Hasta
              </label>
              <input
                id="resumen-hasta"
                type="date"
                value={resumenHasta}
                onChange={(e) => setResumenHasta(e.target.value)}
                className={INPUT_CLASS}
              />
            </div>
            <Button onClick={() => loadResumen(resumenDesde, resumenHasta)} disabled={resumenLoading}>
              Actualizar
            </Button>
            <Button variant="secondary" className="px-3 py-1.5" onClick={exportarCaja}>
              Exportar CSV
            </Button>
          </div>
        </div>

        <div className="p-5">
          {resumenError && (
            <div className="mb-4">
              <ErrorBox message={resumenError} />
            </div>
          )}
          {resumenLoading ? (
            <Loading />
          ) : resumen ? (
            <div className="flex flex-col gap-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-lg border border-neutral-200 p-5 dark:border-neutral-800">
                  <p className="text-sm text-neutral-500">Total cobrado</p>
                  <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">
                    {formatMoney(resumen.totalCobrado)}
                  </p>
                </div>
                <div className="rounded-lg border border-neutral-200 p-5 dark:border-neutral-800">
                  <p className="text-sm text-neutral-500">Cantidad de cobranzas</p>
                  <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">
                    {formatNumber(resumen.cantidadCobranzas)}
                  </p>
                </div>
              </div>

              <div className="grid gap-6 lg:grid-cols-3">
                <ResumenTabla
                  titulo="Por forma de pago"
                  columnas={["Forma de pago", "Monto", "Cantidad"]}
                  filas={resumen.porFormaPago.map((r) => [
                    FORMA_PAGO_LABELS[r.formaPago] ?? r.formaPago,
                    formatMoney(r.monto),
                    formatNumber(r.cantidad),
                  ])}
                />
                <ResumenTabla
                  titulo="Por día"
                  columnas={["Fecha", "Monto", "Cantidad"]}
                  filas={resumen.porDia.map((r) => [
                    formatDate(r.fecha),
                    formatMoney(r.monto),
                    formatNumber(r.cantidad),
                  ])}
                />
                <ResumenTabla
                  titulo="Por vendedor"
                  columnas={["Vendedor", "Monto", "Cantidad"]}
                  filas={resumen.porVendedor.map((r) => [
                    r.vendedorId === -1 ? "Sin vendedor" : r.vendedorNombre,
                    formatMoney(r.monto),
                    formatNumber(r.cantidad),
                  ])}
                />
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Estado de cuenta</h2>
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Cliente
            </label>
            <Combobox
              value={clienteId ? Number(clienteId) : null}
              onChange={handleClienteChange}
              placeholder="Buscar cliente..."
              search={async (q) => {
                const data = await apiGet<PageResponse<Cliente>>(
                  `/api/clientes?q=${encodeURIComponent(q)}&size=20&activos=true`
                );
                return data.content.map((c) => ({
                  id: c.id,
                  label: c.razonSocial,
                  sublabel: c.cuit,
                }));
              }}
            />
          </div>
        </div>

        <div className="p-5">
          {!clienteId ? (
            <p className="text-sm text-neutral-500">Seleccioná un cliente para ver su estado de cuenta.</p>
          ) : cuentaLoading ? (
            <Loading />
          ) : cuentaError ? (
            <ErrorBox message={cuentaError} />
          ) : cuenta ? (
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="rounded-lg border border-neutral-200 p-5 dark:border-neutral-800">
                <p className="text-sm text-neutral-500">Total vendido</p>
                <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">
                  {formatMoney(cuenta.totalVendido)}
                </p>
              </div>
              <div className="rounded-lg border border-neutral-200 p-5 dark:border-neutral-800">
                <p className="text-sm text-neutral-500">Total cobrado</p>
                <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">
                  {formatMoney(cuenta.totalCobrado)}
                </p>
              </div>
              <div
                className={`rounded-lg border p-5 ${
                  cuenta.saldo > 0
                    ? "border-red-200 dark:border-red-900/50"
                    : "border-emerald-200 dark:border-emerald-900/50"
                }`}
              >
                <p className="text-sm text-neutral-500">Saldo</p>
                <p
                  className={`mt-1 text-2xl font-semibold ${
                    cuenta.saldo > 0 ? "text-red-600 dark:text-red-400" : "text-emerald-600 dark:text-emerald-400"
                  }`}
                >
                  {formatMoney(cuenta.saldo)}
                </p>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
        <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Cobranzas</h2>
        </div>
        {cobranzasError && (
          <div className="px-5 pt-5">
            <ErrorBox message={cobranzasError} />
          </div>
        )}
        {cobranzasLoading ? (
          <Loading />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">Cliente</th>
                  <th className="px-4 py-3 font-medium">Pedido</th>
                  <th className="px-4 py-3 font-medium text-right">Monto</th>
                  <th className="px-4 py-3 font-medium">Forma de pago</th>
                  <th className="px-4 py-3 font-medium">Fecha</th>
                  <th className="px-4 py-3 font-medium">Observaciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {cobranzas.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay cobranzas para mostrar
                    </td>
                  </tr>
                )}
                {cobranzas.map((cobranza) => (
                  <tr key={cobranza.id} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                    <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">
                      {clientesById.get(cobranza.clienteId) ?? `Cliente #${cobranza.clienteId}`}
                    </td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
                      {cobranza.pedidoId == null
                        ? "—"
                        : (pedidosById.get(cobranza.pedidoId)?.numero ?? `#${cobranza.pedidoId}`)}
                    </td>
                    <td className="px-4 py-3 text-right text-neutral-700 dark:text-neutral-300">
                      {formatMoney(cobranza.monto)}
                    </td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cobranza.formaPago}</td>
                    <td className="px-4 py-3 text-neutral-500">{formatDateTime(cobranza.fecha)}</td>
                    <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">{cobranza.observaciones ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
        <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
          <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Remitos</h2>
        </div>
        {remitosError && (
          <div className="px-5 pt-5">
            <ErrorBox message={remitosError} />
          </div>
        )}
        {remitosLoading ? (
          <Loading />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
                  <th className="px-4 py-3 font-medium">Número</th>
                  <th className="px-4 py-3 font-medium">Pedido</th>
                  <th className="px-4 py-3 font-medium">Cliente</th>
                  <th className="px-4 py-3 font-medium">Emisión</th>
                  <th className="px-4 py-3 font-medium text-right">Total</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                {remitos.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                      No hay remitos para mostrar
                    </td>
                  </tr>
                )}
                {remitos.map((remito) => (
                  <RemitoRow
                    key={remito.id}
                    remito={remito}
                    clienteNombre={clientesById.get(remito.clienteId)}
                    pedidosById={pedidosById}
                    expanded={expandedRemitoId === remito.id}
                    onToggle={() =>
                      setExpandedRemitoId((prev) => (prev === remito.id ? null : remito.id))
                    }
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {showForm && (
        <RegistrarCobranzaForm
          clientePreseleccionado={clienteId}
          error={formError}
          setError={setFormError}
          submitting={submitting}
          setSubmitting={setSubmitting}
          onClose={() => setShowForm(false)}
          onCreated={handleCobranzaCreated}
        />
      )}
    </div>
  );
}

function ResumenTabla({
  titulo,
  columnas,
  filas,
}: {
  titulo: string;
  columnas: string[];
  filas: string[][];
}) {
  return (
    <div className="overflow-hidden rounded-lg border border-neutral-200 dark:border-neutral-800">
      <div className="border-b border-neutral-200 bg-neutral-50 px-4 py-2.5 dark:border-neutral-800 dark:bg-neutral-800/50">
        <h3 className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{titulo}</h3>
      </div>
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-neutral-200 text-xs uppercase tracking-wide text-neutral-500 dark:border-neutral-800">
            {columnas.map((col, i) => (
              <th key={col} className={`px-4 py-2 font-medium ${i > 0 ? "text-right" : ""}`}>
                {col}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
          {filas.length === 0 && (
            <tr>
              <td colSpan={columnas.length} className="px-4 py-6 text-center text-neutral-500">
                Sin datos
              </td>
            </tr>
          )}
          {filas.map((fila, i) => (
            <tr key={i} className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
              {fila.map((celda, j) => (
                <td
                  key={j}
                  className={`px-4 py-2.5 ${
                    j === 0
                      ? "font-medium text-neutral-900 dark:text-neutral-100"
                      : "text-right text-neutral-600 dark:text-neutral-400"
                  }`}
                >
                  {celda}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function RemitoRow({
  remito,
  clienteNombre,
  pedidosById,
  expanded,
  onToggle,
}: {
  remito: Remito;
  clienteNombre: string | undefined;
  pedidosById: Map<number, Pedido>;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <>
      <tr className="transition-colors hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
        <td className="px-4 py-3 font-medium text-neutral-900 dark:text-neutral-100">{remito.numero}</td>
        <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
          {pedidosById.get(remito.pedidoId)?.numero ?? `#${remito.pedidoId}`}
        </td>
        <td className="px-4 py-3 text-neutral-600 dark:text-neutral-400">
          {clienteNombre ?? `Cliente #${remito.clienteId}`}
        </td>
        <td className="px-4 py-3 text-neutral-500">{formatDate(remito.fechaEmision)}</td>
        <td className="px-4 py-3 text-right text-neutral-700 dark:text-neutral-300">{formatMoney(remito.montoTotal)}</td>
        <td className="px-4 py-3 text-right">
          <Button variant="secondary" onClick={onToggle}>
            {expanded ? "Ocultar" : "Detalle"}
          </Button>
        </td>
      </tr>
      {expanded && (
        <tr className="bg-neutral-50 dark:bg-neutral-800/30">
          <td colSpan={6} className="px-4 py-4">
            <div className="flex flex-col gap-2">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-neutral-500">Líneas</h3>
              <div className="overflow-hidden rounded-md border border-neutral-200 dark:border-neutral-800">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-neutral-200 bg-white text-xs uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900">
                      <th className="px-3 py-2 font-medium text-left">Item</th>
                      <th className="px-3 py-2 font-medium text-right">Cantidad</th>
                      <th className="px-3 py-2 font-medium text-right">Precio unitario</th>
                      <th className="px-3 py-2 font-medium text-right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800">
                    {remito.lineas.map((linea) => (
                      <tr key={linea.id}>
                        <td className="px-3 py-2 text-neutral-700 dark:text-neutral-300">#{linea.itemId}</td>
                        <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                          {formatNumber(linea.cantidad)}
                        </td>
                        <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                          {formatMoney(linea.precioUnitario)}
                        </td>
                        <td className="px-3 py-2 text-right text-neutral-700 dark:text-neutral-300">
                          {formatMoney(linea.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

function RegistrarCobranzaForm({
  clientePreseleccionado,
  error,
  setError,
  submitting,
  setSubmitting,
  onClose,
  onCreated,
}: {
  clientePreseleccionado: string;
  error: string | null;
  setError: (value: string | null) => void;
  submitting: boolean;
  setSubmitting: (value: boolean) => void;
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [clienteId, setClienteId] = useState<number | null>(
    clientePreseleccionado ? Number(clientePreseleccionado) : null
  );
  const [monto, setMonto] = useState("");
  const [formaPago, setFormaPago] = useState("");
  const [pedidoId, setPedidoId] = useState("");
  const [observaciones, setObservaciones] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);

    const montoNum = Number(monto);
    if (clienteId == null) {
      setError("Seleccioná un cliente.");
      return;
    }
    if (!monto || !Number.isFinite(montoNum) || montoNum <= 0) {
      setError("Ingresá un monto mayor a cero.");
      return;
    }
    if (!formaPago) {
      setError("Seleccioná una forma de pago.");
      return;
    }

    setSubmitting(true);
    try {
      await apiPost("/api/cobranzas", {
        clienteId,
        pedidoId: pedidoId ? Number(pedidoId) : undefined,
        monto: montoNum,
        formaPago,
        observaciones: observaciones.trim() || undefined,
      });
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Registrar cobranza" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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
              sublabel: c.cuit,
            }));
          }}
        />

        <div className="grid gap-4 sm:grid-cols-2">
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
              className={INPUT_CLASS}
              placeholder="0.00"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="cobranza-formaPago" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
              Forma de pago
            </label>
            <select
              id="cobranza-formaPago"
              value={formaPago}
              onChange={(e) => setFormaPago(e.target.value)}
              className={INPUT_CLASS}
            >
              <option value="">Seleccionar...</option>
              {FORMAS_PAGO.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cobranza-pedido" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            N° de pedido (opcional)
          </label>
          <input
            id="cobranza-pedido"
            type="number"
            min="1"
            step="1"
            value={pedidoId}
            onChange={(e) => setPedidoId(e.target.value)}
            className={INPUT_CLASS}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="cobranza-observaciones" className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
            Observaciones
          </label>
          <textarea
            id="cobranza-observaciones"
            value={observaciones}
            onChange={(e) => setObservaciones(e.target.value)}
            rows={2}
            className={INPUT_CLASS}
          />
        </div>

        {error && <ErrorBox message={error} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Registrando..." : "Registrar cobranza"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
