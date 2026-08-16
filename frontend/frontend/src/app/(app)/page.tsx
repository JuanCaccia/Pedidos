"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import type { Item, Lote, Pedido, ReporteStockItem, ReporteVenta, PageResponse } from "@/lib/types";
import { formatDate, formatDateTime, formatMoney, formatNumber } from "@/lib/format";
import EstadoBadge from "@/components/EstadoBadge";
import Loading from "@/components/Loading";
import ErrorBox from "@/components/ErrorBox";
import Button from "@/components/Button";

const DIAS_POR_VENCER = 30;

const ESTADOS_STOCK_PENDIENTE = new Set<Pedido["estado"]>([
  "PENDIENTE_STOCK",
  "PENDIENTE_PREPARACION",
  "PENDIENTE_ENTREGA",
  "RE_AGENDADO",
]);

interface DashboardData {
  stock: ReporteStockItem[];
  ventas: ReporteVenta[];
  pedidos: Pedido[];
}

interface AlertaStockBajo {
  itemId: number;
  sku: string;
  nombre: string;
  disponible: number;
  stockMinimo: number;
}

interface AlertaLote {
  id: number;
  itemNombre: string;
  codigoLote: string;
  fechaVencimiento: string;
  vencido: boolean;
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [bajoStock, setBajoStock] = useState<AlertaStockBajo[]>([]);
  const [pedidosPendientes, setPedidosPendientes] = useState<Pedido[]>([]);
  const [lotesPorVencer, setLotesPorVencer] = useState<AlertaLote[]>([]);
  const [reagendados, setReagendados] = useState<Pedido[]>([]);

  const cancelledRef = useRef(false);

  const cargarTodo = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [stock, ventas, pedidos, items, lotes, reagendadosData] = await Promise.all([
        apiGet<ReporteStockItem[]>("/api/reportes/stock"),
        apiGet<ReporteVenta[]>("/api/reportes/ventas"),
        apiGet<PageResponse<Pedido>>("/api/pedidos?size=500"),
        apiGet<PageResponse<Item>>("/api/items?size=500"),
        apiGet<Lote[]>(`/api/stock/lotes/por-vencer?dias=${DIAS_POR_VENCER}`),
        apiGet<PageResponse<Pedido>>("/api/pedidos?estado=RE_AGENDADO&size=500"),
      ]);
      if (cancelledRef.current) return;

      const disponiblePorItem = new Map(stock.map((s) => [s.itemId, s.disponible]));
      const nombrePorItem = new Map(items.content.map((i) => [i.id, i.nombre]));
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);

      setData({ stock, ventas, pedidos: pedidos.content });

      setReagendados(reagendadosData.content);

      setBajoStock(
        items.content
          .map((i) => ({
            itemId: i.id,
            sku: i.sku,
            nombre: i.nombre,
            disponible: disponiblePorItem.get(i.id) ?? 0,
            stockMinimo: i.stockMinimo,
          }))
          .filter((a) => a.disponible <= a.stockMinimo)
          .sort((a, b) => a.disponible - b.disponible)
      );

      setPedidosPendientes(
        pedidos.content.filter(
          (p) => ESTADOS_STOCK_PENDIENTE.has(p.estado) && p.items.some((i) => i.pendienteStock)
        )
      );

      setLotesPorVencer(
        lotes
          .filter((l) => l.fechaVencimiento)
          .map((l) => ({
            id: l.id,
            itemNombre: nombrePorItem.get(l.itemId) ?? `Item #${l.itemId}`,
            codigoLote: l.codigoLote,
            fechaVencimiento: l.fechaVencimiento as string,
            vencido: new Date(l.fechaVencimiento as string).getTime() < hoy.getTime(),
          }))
          .sort(
            (a, b) =>
              new Date(a.fechaVencimiento).getTime() - new Date(b.fechaVencimiento).getTime()
          )
      );
    } catch (err) {
      if (!cancelledRef.current) setError(err instanceof Error ? err.message : "Error inesperado");
    } finally {
      if (!cancelledRef.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    cancelledRef.current = false;
    cargarTodo();
    return () => {
      cancelledRef.current = true;
    };
  }, [cargarTodo]);

  if (error) {
    return (
      <div className="flex flex-col gap-4">
        <ErrorBox message={error} />
        <div>
          <Button variant="secondary" onClick={cargarTodo}>
            Reintentar
          </Button>
        </div>
      </div>
    );
  }
  if (!data) return <Loading />;

  const menosDisponible = [...data.stock]
    .sort((a, b) => a.disponible - b.disponible)
    .slice(0, 5);
  const ventasTotales = data.ventas.reduce(
    (acc, v) => ({
      cantidadPedidos: acc.cantidadPedidos + v.cantidadPedidos,
      cantidadUnidades: acc.cantidadUnidades + v.cantidadUnidades,
      monto: acc.monto + v.monto,
    }),
    { cantidadPedidos: 0, cantidadUnidades: 0, monto: 0 }
  );
  const pedidosRecientes = [...data.pedidos]
    .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
    .slice(0, 5);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Panel</h1>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-lg border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-neutral-500">Total de items</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">{data.stock.length}</p>
        </div>
        <div className="rounded-lg border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-neutral-500">Ventas (pedidos)</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">{ventasTotales.cantidadPedidos}</p>
        </div>
        <div className="rounded-lg border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
          <p className="text-sm text-neutral-500">Monto total vendido</p>
          <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">{formatMoney(ventasTotales.monto)}</p>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <AlertaCard
          titulo="Stock bajo"
          enlaceLabel="Ver items"
          enlaceHref="/stock?filtro=bajo"
          loading={loading}
          error={error}
          onReintentar={cargarTodo}
        >
          {bajoStock.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">Sin items con stock bajo.</p>
          ) : (
            bajoStock.map((item) => (
              <div key={item.itemId} className="flex items-center justify-between gap-4 px-5 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">
                    {item.nombre}
                  </p>
                  <p className="text-xs text-neutral-500">
                    SKU {item.sku} · mín. {formatNumber(item.stockMinimo)}
                  </p>
                </div>
                <span
                  className={`rounded-md px-2 py-1 text-sm font-semibold ${
                    item.disponible === 0
                      ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"
                      : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"
                  }`}
                >
                  {formatNumber(item.disponible)}
                </span>
              </div>
            ))
          )}
        </AlertaCard>

        <AlertaCard
          titulo="Pedidos con stock pendiente"
          enlaceLabel="Ver pedidos"
          enlaceHref="/pedidos?tab=PENDIENTE_STOCK"
          loading={loading}
          error={error}
          onReintentar={cargarTodo}
        >
          {pedidosPendientes.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos pendientes de stock.</p>
          ) : (
            pedidosPendientes.map((pedido) => (
              <div key={pedido.id} className="flex items-center justify-between gap-4 px-5 py-3">
                <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
                  {pedido.numero}
                </p>
                <EstadoBadge estado={pedido.estado} />
              </div>
            ))
          )}
        </AlertaCard>

        <AlertaCard
          titulo="Lotes por vencer"
          enlaceLabel="Ver stock"
          enlaceHref="/stock?tab=lotes&filtro=vencer"
          loading={loading}
          error={error}
          onReintentar={cargarTodo}
        >
          {lotesPorVencer.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">
              No hay lotes por vencer en los próximos {DIAS_POR_VENCER} días.
            </p>
          ) : (
            lotesPorVencer.map((lote) => (
              <div key={lote.id} className="flex items-center justify-between gap-4 px-5 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">
                    {lote.itemNombre}
                  </p>
                  <p className="text-xs text-neutral-500">Lote {lote.codigoLote}</p>
                </div>
                <span
                  className={`rounded-md px-2 py-1 text-sm font-semibold whitespace-nowrap ${
                    lote.vencido
                      ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"
                      : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"
                  }`}
                >
                  {lote.vencido ? `Vencido · ${formatDate(lote.fechaVencimiento)}` : formatDate(lote.fechaVencimiento)}
                </span>
              </div>
            ))
          )}
        </AlertaCard>

        <AlertaCard
          titulo="Pedidos re-agendados"
          enlaceLabel="Ver pedidos"
          enlaceHref="/pedidos?tab=RE_AGENDADO"
          loading={loading}
          error={error}
          onReintentar={cargarTodo}
        >
          {reagendados.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos re-agendados.</p>
          ) : (
            reagendados.map((pedido) => {
              const dias = pedido.updatedAt
                ? Math.floor((Date.now() - new Date(pedido.updatedAt).getTime()) / 86400000)
                : null;
              const vencido = dias !== null && dias >= 7;
              return (
                <div key={pedido.id} className="flex items-center justify-between gap-4 px-5 py-3">
                  <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
                    {pedido.numero}
                  </p>
                  <span
                    className={`rounded-md px-2 py-1 text-sm font-semibold whitespace-nowrap ${
                      vencido
                        ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"
                        : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"
                    }`}
                  >
                    {dias === null ? "-" : `hace ${dias} día${dias === 1 ? "" : "s"}`}
                  </span>
                </div>
              );
            })
          )}
        </AlertaCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
            <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Stock con menos disponible</h2>
          </div>
          <div className="divide-y divide-neutral-100 dark:divide-neutral-800">
            {menosDisponible.length === 0 && (
              <p className="px-5 py-4 text-sm text-neutral-500">Sin datos de stock</p>
            )}
            {menosDisponible.map((item) => (
              <div key={item.itemId} className="flex items-center justify-between px-5 py-3">
                <div>
                  <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{item.nombre}</p>
                  <p className="text-xs text-neutral-500">SKU {item.sku}</p>
                </div>
                <span
                  className={`text-sm font-semibold ${
                    item.disponible <= 0
                      ? "text-red-600 dark:text-red-400"
                      : "text-neutral-700 dark:text-neutral-300"
                  }`}
                >
                  {formatNumber(item.disponible)}
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
            <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Pedidos recientes</h2>
          </div>
          <div className="divide-y divide-neutral-100 dark:divide-neutral-800">
            {pedidosRecientes.length === 0 && (
              <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos</p>
            )}
            {pedidosRecientes.map((pedido) => (
              <div key={pedido.id} className="flex items-center justify-between gap-4 px-5 py-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{pedido.numero}</p>
                  <p className="text-xs text-neutral-500">{formatDateTime(pedido.fechaCreacion)}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">{formatMoney(pedido.total)}</span>
                  <EstadoBadge estado={pedido.estado} />
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function AlertaCard({
  titulo,
  enlaceLabel,
  enlaceHref,
  loading,
  error,
  onReintentar,
  children,
}: {
  titulo: string;
  enlaceLabel: string;
  enlaceHref: string;
  loading: boolean;
  error: string | null;
  onReintentar: () => void;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex items-center justify-between border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
        <h2 className="font-medium text-neutral-900 dark:text-neutral-100">{titulo}</h2>
        <Link
          href={enlaceHref}
          className="text-xs font-medium text-blue-600 transition-colors hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
        >
          {enlaceLabel}
        </Link>
      </div>
      <div className="divide-y divide-neutral-100 dark:divide-neutral-800">
        {loading && <Loading />}
        {!loading && error && (
          <div className="px-5 py-4">
            <ErrorBox message={error} />
            <button
              onClick={onReintentar}
              className="mt-2 text-xs font-medium text-blue-600 transition-colors hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
            >
              Reintentar
            </button>
          </div>
        )}
        {!loading && !error && children}
      </div>
    </section>
  );
}
