import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Pedidos — Repartidor",
    short_name: "Repartidor",
    description: "Mis entregas del día",
    start_url: "/turno",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#2563eb",
    icons: [{ src: "/icon.svg", sizes: "any", type: "image/svg+xml" }],
  };
}
