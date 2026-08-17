"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { apiGet } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { Item, Lote, Pedido, ReporteStockItem, ReporteVenta, PageResponse } from "@/lib/types";
import { formatDate, formatDateTime, formatMoney, formatNumber } from "@/lib/format";
import EstadoBadge from "@/components/EstadoBadge";
import Loading from "@/components/Loading";

const DIAS_POR_VENCER = 30;

const ESTADOS_STOCK_PENDIENTE = new Set<Pedido["estado"]>([
  "PENDIENTE_STOCK",
  "PENDIENTE_PREPARACION",
  "PENDIENTE_ENTREGA",
  "RE_AGENDADO",
]);

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

interface WidgetPlan {
  stock: boolean;
  ventas: boolean;
  items: boolean;
  lotes: boolean;
}

async function safeGet<T>(loader: () => Promise<T>): Promise<T | null> {
  try {
    return await loader();
  } catch {
    return null;
  }
}

export default function DashboardPage() {
  const { user } = useAuth();
  const router = useRouter();

  const roles = user?.roles ?? [];
  const esAdmin = roles.includes("ADMINISTRATIVO");
  const esEncargado = roles.includes("ENCARGADO_DEPOSITO");
  const esRepartidor = roles.includes("REPARTIDOR");

  const plan: WidgetPlan = useMemo(
    () => ({
      stock: esAdmin || esEncargado,
      ventas: esAdmin,
      items: esAdmin || esEncargado,
      lotes: esAdmin || esEncargado,
    }),
    [esAdmin, esEncargado]
  );

  const [stock, setStock] = useState<ReporteStockItem[] | null>(null);
  const [ventas, setVentas] = useState<ReporteVenta[] | null>(null);
  const [pedidos, setPedidos] = useState<Pedido[] | null>(null);
  const [items, setItems] = useState<Item[] | null>(null);
  const [lotes, setLotes] = useState<Lote[] | null>(null);
  const [reagendados, setReagendados] = useState<Pedido[] | null>(null);
  const [loading, setLoading] = useState(true);

  const cancelledRef = useRef(false);

  const cargarTodo = useCallback(async () => {
    setLoading(true);
    const results = await Promise.all([
      plan.stock ? safeGet(() => apiGet<ReporteStockItem[]>("/api/reportes/stock")) : Promise.resolve(null),
      plan.ventas ? safeGet(() => apiGet<ReporteVenta[]>("/api/reportes/ventas")) : Promise.resolve(null),
      safeGet(() => apiGet<PageResponse<Pedido>>("/api/pedidos?size=500")),
      plan.items ? safeGet(() => apiGet<PageResponse<Item>>("/api/items?size=500")) : Promise.resolve(null),
      plan.lotes ? safeGet(() => apiGet<Lote[]>(`/api/stock/lotes/por-vencer?dias=${DIAS_POR_VENCER}`)) : Promise.resolve(null),
      safeGet(() => apiGet<PageResponse<Pedido>>("/api/pedidos?estado=RE_AGENDADO&size=500")),
    ]);
    if (cancelledRef.current) return;
    setStock(results[0]);
    setVentas(results[1]);
    setPedidos(results[2] ? results[2].content : null);
    setItems(results[3] ? results[3].content : null);
    setLotes(results[4]);
    setReagendados(results[5] ? results[5].content : null);
    setLoading(false);
  }, [plan]);

  useEffect(() => {
    if (esRepartidor) {
      router.replace("/turno");
      return;
    }
    cancelledRef.current = false;
    cargarTodo();
    return () => {
      cancelledRef.current = true;
    };
  }, [esRepartidor, router, cargarTodo]);

  const disponiblePorItem = useMemo(
    () => new Map((stock ?? []).map((s) => [s.itemId, s.disponible])),
    [stock]
  );
  const nombrePorItem = useMemo(
    () => new Map((items ?? []).map((i) => [i.id, i.nombre])),
    [items]
  );

  const hoy = useMemo(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }, []);

  const bajoStock: AlertaStockBajo[] = useMemo(() => {
    if (!stock || !items) return [];
    return items
      .map((i) => ({
        itemId: i.id,
        sku: i.sku,
        nombre: i.nombre,
        disponible: disponiblePorItem.get(i.id) ?? 0,
        stockMinimo: i.stockMinimo,
      }))
      .filter((a) => a.disponible <= a.stockMinimo)
      .sort((a, b) => a.disponible - b.disponible);
  }, [stock, items, disponiblePorItem]);

  const pedidosPendientes: Pedido[] = useMemo(
    () =>
      (pedidos ?? []).filter(
        (p) => ESTADOS_STOCK_PENDIENTE.has(p.estado) && p.items.some((i) => i.pendienteStock)
      ),
    [pedidos]
  );

  const lotesPorVencer: AlertaLote[] = useMemo(() => {
    if (!lotes) return [];
    return lotes
      .filter((l) => l.fechaVencimiento)
      .map((l) => ({
        id: l.id,
        itemNombre: nombrePorItem.get(l.itemId) ?? `Item #${l.itemId}`,
        codigoLote: l.codigoLote,
        fechaVencimiento: l.fechaVencimiento as string,
        vencido: new Date(l.fechaVencimiento as string).getTime() < hoy.getTime(),
      }))
      .sort(
        (a, b) => new Date(a.fechaVencimiento).getTime() - new Date(b.fechaVencimiento).getTime()
      );
  }, [lotes, nombrePorItem, hoy]);

  const reagendadosWidget: Pedido[] = useMemo(() => reagendados ?? [], [reagendados]);

  const menosDisponible: ReporteStockItem[] = useMemo(
    () => (stock ? [...stock].sort((a, b) => a.disponible - b.disponible).slice(0, 5) : []),
    [stock]
  );

  const pedidosRecientes: Pedido[] = useMemo(
    () =>
      pedidos
        ? [...pedidos]
            .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
            .slice(0, 5)
        : [],
    [pedidos]
  );

  const ventasTotales = useMemo(() => {
    if (!ventas) return null;
    return ventas.reduce(
      (acc, v) => ({
        cantidadPedidos: acc.cantidadPedidos + v.cantidadPedidos,
        cantidadUnidades: acc.cantidadUnidades + v.cantidadUnidades,
        monto: acc.monto + v.monto,
      }),
      { cantidadPedidos: 0, cantidadUnidades: 0, monto: 0 }
    );
  }, [ventas]);

  if (!user) return null;

  if (esRepartidor) {
    return <Loading label="Redirigiendo a Mi Jornada..." />;
  }

  if (loading) return <Loading />;

  const stats = buildStats({ esAdmin, esEncargado, esVendedor: roles.includes("VENDEDOR"), stock, ventasTotales, pedidos, pedidosPendientes, reagendadosWidget, lotesPorVencer });

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">Panel</h1>

      <div className="grid gap-4 sm:grid-cols-3">
        {stats.map((s) => (
          <div key={s.label} className="rounded-lg border border-neutral-200 bg-white p-5 dark:border-neutral-800 dark:bg-neutral-900">
            <p className="text-sm text-neutral-500">{s.label}</p>
            <p className="mt-1 text-2xl font-semibold text-neutral-900 dark:text-neutral-100">{s.value}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {plan.stock && (
          <AlertaCard titulo="Stock bajo" enlaceLabel="Ver items" enlaceHref="/stock?filtro=bajo">
            {stock === null ? (
              <EmptyMensaje texto="No disponible" />
            ) : bajoStock.length === 0 ? (
              <p className="px-5 py-4 text-sm text-neutral-500">Sin items con stock bajo.</p>
            ) : (
              bajoStock.map((item) => (
                <div key={item.itemId} className="flex items-center justify-between gap-4 px-5 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">{item.nombre}</p>
                    <p className="text-xs text-neutral-500">SKU {item.sku} · mín. {formatNumber(item.stockMinimo)}</p>
                  </div>
                  <span className={`rounded-md px-2 py-1 text-sm font-semibold ${item.disponible === 0 ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"}`}>
                    {formatNumber(item.disponible)}
                  </span>
                </div>
              ))
            )}
          </AlertaCard>
        )}

        <AlertaCard titulo="Pedidos con stock pendiente" enlaceLabel="Ver pedidos" enlaceHref="/pedidos?tab=PENDIENTE_STOCK">
          {pedidos === null ? (
            <EmptyMensaje texto="No disponible" />
          ) : pedidosPendientes.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos pendientes de stock.</p>
          ) : (
            pedidosPendientes.map((pedido) => (
              <div key={pedido.id} className="flex items-center justify-between gap-4 px-5 py-3">
                <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{pedido.numero}</p>
                <EstadoBadge estado={pedido.estado} />
              </div>
            ))
          )}
        </AlertaCard>

        {plan.lotes && (
          <AlertaCard titulo="Lotes por vencer" enlaceLabel="Ver stock" enlaceHref="/stock?tab=lotes&filtro=vencer">
            {lotes === null ? (
              <EmptyMensaje texto="No disponible" />
            ) : lotesPorVencer.length === 0 ? (
              <p className="px-5 py-4 text-sm text-neutral-500">No hay lotes por vencer en los próximos {DIAS_POR_VENCER} días.</p>
            ) : (
              lotesPorVencer.map((lote) => (
                <div key={lote.id} className="flex items-center justify-between gap-4 px-5 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-neutral-900 dark:text-neutral-100">{lote.itemNombre}</p>
                    <p className="text-xs text-neutral-500">Lote {lote.codigoLote}</p>
                  </div>
                  <span className={`rounded-md px-2 py-1 text-sm font-semibold whitespace-nowrap ${lote.vencido ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"}`}>
                    {lote.vencido ? `Vencido · ${formatDate(lote.fechaVencimiento)}` : formatDate(lote.fechaVencimiento)}
                  </span>
                </div>
              ))
            )}
          </AlertaCard>
        )}

        <AlertaCard titulo="Pedidos re-agendados" enlaceLabel="Ver pedidos" enlaceHref="/pedidos?tab=RE_AGENDADO">
          {reagendados === null ? (
            <EmptyMensaje texto="No disponible" />
          ) : reagendadosWidget.length === 0 ? (
            <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos re-agendados.</p>
          ) : (
            reagendadosWidget.map((pedido) => {
              const dias = pedido.updatedAt ? Math.floor((Date.now() - new Date(pedido.updatedAt).getTime()) / 86400000) : null;
              const vencido = dias !== null && dias >= 7;
              return (
                <div key={pedido.id} className="flex items-center justify-between gap-4 px-5 py-3">
                  <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{pedido.numero}</p>
                  <span className={`rounded-md px-2 py-1 text-sm font-semibold whitespace-nowrap ${vencido ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" : "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300"}`}>
                    {dias === null ? "-" : `hace ${dias} día${dias === 1 ? "" : "s"}`}
                  </span>
                </div>
              );
            })
          )}
        </AlertaCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {plan.stock && (
          <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
            <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
              <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Stock con menos disponible</h2>
            </div>
            <div className="divide-y divide-neutral-100 dark:divide-neutral-800">
              {stock === null ? (
                <EmptyMensaje texto="No disponible" />
              ) : menosDisponible.length === 0 ? (
                <p className="px-5 py-4 text-sm text-neutral-500">Sin datos de stock</p>
              ) : (
                menosDisponible.map((item) => (
                  <div key={item.itemId} className="flex items-center justify-between px-5 py-3">
                    <div>
                      <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">{item.nombre}</p>
                      <p className="text-xs text-neutral-500">SKU {item.sku}</p>
                    </div>
                    <span className={`text-sm font-semibold ${item.disponible <= 0 ? "text-red-600 dark:text-red-400" : "text-neutral-700 dark:text-neutral-300"}`}>
                      {formatNumber(item.disponible)}
                    </span>
                  </div>
                ))
              )}
            </div>
          </section>
        )}

        <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
          <div className="border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
            <h2 className="font-medium text-neutral-900 dark:text-neutral-100">Pedidos recientes</h2>
          </div>
          <div className="divide-y divide-neutral-100 dark:divide-neutral-800">
            {pedidos === null ? (
              <EmptyMensaje texto="No disponible" />
            ) : pedidosRecientes.length === 0 ? (
              <p className="px-5 py-4 text-sm text-neutral-500">Sin pedidos</p>
            ) : (
              pedidosRecientes.map((pedido) => (
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
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

function buildStats({
  esAdmin,
  esEncargado,
  esVendedor,
  stock,
  ventasTotales,
  pedidos,
  pedidosPendientes,
  reagendadosWidget,
  lotesPorVencer,
}: {
  esAdmin: boolean;
  esEncargado: boolean;
  esVendedor: boolean;
  stock: ReporteStockItem[] | null;
  ventasTotales: { cantidadPedidos: number; cantidadUnidades: number; monto: number } | null;
  pedidos: Pedido[] | null;
  pedidosPendientes: Pedido[];
  reagendadosWidget: Pedido[];
  lotesPorVencer: AlertaLote[];
}): { label: string; value: string }[] {
  const totalPedidos = pedidos?.length ?? 0;
  if (esAdmin) {
    return [
      { label: "Total de items", value: stock ? String(stock.length) : "—" },
      { label: "Ventas (pedidos)", value: ventasTotales ? String(ventasTotales.cantidadPedidos) : "—" },
      { label: "Monto total vendido", value: ventasTotales ? formatMoney(ventasTotales.monto) : "—" },
    ];
  }
  if (esEncargado) {
    return [
      { label: "Total de items", value: stock ? String(stock.length) : "—" },
      { label: "Pedidos", value: String(totalPedidos) },
      { label: "Lotes por vencer", value: String(lotesPorVencer.length) },
    ];
  }
  if (esVendedor) {
    return [
      { label: "Total de pedidos", value: String(totalPedidos) },
      { label: "Con stock pendiente", value: String(pedidosPendientes.length) },
      { label: "Re-agendados", value: String(reagendadosWidget.length) },
    ];
  }
  return [{ label: "Pedidos", value: String(totalPedidos) }];
}

function EmptyMensaje({ texto }: { texto: string }) {
  return <p className="px-5 py-4 text-sm text-neutral-400 dark:text-neutral-500">{texto}</p>;
}

function AlertaCard({
  titulo,
  enlaceLabel,
  enlaceHref,
  children,
}: {
  titulo: string;
  enlaceLabel: string;
  enlaceHref: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-lg border border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900">
      <div className="flex items-center justify-between border-b border-neutral-200 px-5 py-3.5 dark:border-neutral-800">
        <h2 className="font-medium text-neutral-900 dark:text-neutral-100">{titulo}</h2>
        <Link href={enlaceHref} className="text-xs font-medium text-blue-600 transition-colors hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300">
          {enlaceLabel}
        </Link>
      </div>
      <div className="divide-y divide-neutral-100 dark:divide-neutral-800">{children}</div>
    </section>
  );
}
