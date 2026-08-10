package com.sistema.reporte.adapter.in.web;

import com.sistema.reporte.port.in.ConsultarReportes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reportes")
@Tag(name = "Reportes")
public class ReporteController {

	private final ConsultarReportes consultarReportes;

	public ReporteController(ConsultarReportes consultarReportes) {
		this.consultarReportes = consultarReportes;
	}

	@GetMapping("/stock")
	@Operation(summary = "Stock general: disponible y reservas activas por item")
	public List<ConsultarReportes.ItemStockReporte> stock() {
		return consultarReportes.stockGeneral();
	}

	@GetMapping("/ventas")
	@Operation(summary = "Ventas por vendedor (pedidos ENTREGADO y ENTREGADO_PARCIAL)")
	public List<ConsultarReportes.VentaVendedorReporte> ventas(
			@RequestParam(required = false) Long vendedorId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return consultarReportes.ventasPorVendedor(vendedorId, desde, hasta);
	}

	@GetMapping("/rutas")
	@Operation(summary = "Rutas por fecha (o todas si no se pasa fecha)")
	public List<ConsultarReportes.RutaReporte> rutas(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return consultarReportes.rutasPorFecha(fecha);
	}
}
