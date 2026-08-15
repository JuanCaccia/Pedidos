export function descargarCSV(texto: string, nombreArchivo: string): void {
  const blob = new Blob(["\uFEFF" + texto], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = nombreArchivo;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export async function exportarCSV(path: string, nombreArchivo: string): Promise<void> {
  const texto = await (await import("@/lib/api")).apiDownloadText(path);
  descargarCSV(texto, nombreArchivo);
}
