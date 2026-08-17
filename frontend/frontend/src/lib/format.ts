export function formatMoney(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return "-";
  const n = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(n)) return "-";
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 2,
  }).format(n);
}

export function formatNumber(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return "-";
  const n = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(n)) return "-";
  return new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 }).format(n);
}

function parseDateLocal(value: string): Date {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (match) {
    return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  }
  return new Date(value);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "-";
  const d = parseDateLocal(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "-";
  const d = parseDateLocal(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}
