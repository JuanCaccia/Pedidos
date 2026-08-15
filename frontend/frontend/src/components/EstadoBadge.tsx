import type { EstadoPedido } from "@/lib/types";

const ESTADO_STYLES: Record<EstadoPedido, { badge: string; label: string }> = {
  PENDIENTE_CONFIRMACION: { badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300", label: "Pendiente confirmación" },
  PENDIENTE_PREPARACION: { badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300", label: "Pendiente preparación" },
  PENDIENTE_STOCK: { badge: "bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300", label: "Pendiente de stock" },
  PENDIENTE_ENTREGA: { badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300", label: "Pendiente entrega" },
  EN_VIAJE: { badge: "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300", label: "En viaje" },
  ENTREGADO: { badge: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300", label: "Entregado" },
  ENTREGADO_PARCIAL: { badge: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300", label: "Entregado parcial" },
  RE_AGENDADO: { badge: "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300", label: "Re-agendado" },
  RECHAZADO: { badge: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300", label: "Rechazado" },
};

export default function EstadoBadge({ estado }: { estado: EstadoPedido }) {
  const { badge, label } = ESTADO_STYLES[estado] ?? {
    badge: "bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300",
    label: estado,
  };
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ${badge}`}>
      {label}
    </span>
  );
}
