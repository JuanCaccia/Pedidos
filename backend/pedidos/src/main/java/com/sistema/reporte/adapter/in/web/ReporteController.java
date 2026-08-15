package com.sistema.reporte.adapter.in.web;

import com.sistema.common.util.CsvWriter;
import com.sistema.reporte.port.in.ConsultarReportes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

	@GetMapping("/stock/exportar.csv")
	@Operation(summary = "Exporta el stock general a CSV")
	public ResponseEntity<byte[]> stockCsv() {
		List<String> headers = List.of("itemId", "sku", "nombre", "disponible", "reservasActivas");
		List<List<String>> filas = new ArrayList<>();
		for (ConsultarReportes.ItemStockReporte r : consultarReportes.stockGeneral()) {
			filas.add(List.of(String.valueOf(r.itemId()), r.sku(), r.nombre(),
					r.disponible() == null ? "" : r.disponible().toPlainString(),
					r.reservasActivas() == null ? "" : r.reservasActivas().toPlainString()));
		}
		return csv("stock", headers, filas);
	}

	@GetMapping("/ventas")
	@Operation(summary = "Ventas por vendedor (pedidos ENTREGADO y ENTREGADO_PARCIAL)")
	public List<ConsultarReportes.VentaVendedorReporte> ventas(
			@RequestParam(required = false) Long vendedorId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return consultarReportes.ventasPorVendedor(vendedorId, desde, hasta);
	}

	@GetMapping("/ventas/exportar.csv")
	@Operation(summary = "Exporta las ventas por vendedor a CSV")
	public ResponseEntity<byte[]> ventasCsv(
			@RequestParam(required = false) Long vendedorId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		List<String> headers = List.of("vendedorId", "vendedorNombre", "cantidadPedidos", "cantidadUnidades", "monto");
		List<List<String>> filas = new ArrayList<>();
		for (ConsultarReportes.VentaVendedorReporte r : consultarReportes.ventasPorVendedor(vendedorId, desde, hasta)) {
			filas.add(List.of(String.valueOf(r.vendedorId()), r.vendedorNombre(),
					String.valueOf(r.cantidadPedidos()),
					r.cantidadUnidades() == null ? "" : r.cantidadUnidades().toPlainString(),
					r.monto() == null ? "" : r.monto().toPlainString()));
		}
		return csv("ventas", headers, filas);
	}

	@GetMapping("/rutas")
	@Operation(summary = "Rutas por fecha (o todas si no se pasa fecha)")
	public List<ConsultarReportes.RutaReporte> rutas(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return consultarReportes.rutasPorFecha(fecha);
	}

	@GetMapping("/caja")
	@Operation(summary = "Resumen de caja: cobranzas por forma de pago, día y vendedor")
	public ConsultarReportes.ResumenCaja caja(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return consultarReportes.resumenCaja(desde, hasta);
	}

	@GetMapping("/caja/exportar.csv")
	@Operation(summary = "Exporta el resumen de caja por día a CSV")
	public ResponseEntity<byte[]> cajaCsv(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		List<String> headers = List.of("fecha", "monto", "cantidad");
		List<List<String>> filas = new ArrayList<>();
		for (ConsultarReportes.PorDia d : consultarReportes.resumenCaja(desde, hasta).porDia()) {
			filas.add(List.of(String.valueOf(d.fecha()),
					d.monto() == null ? "" : d.monto().toPlainString(),
					String.valueOf(d.cantidad())));
		}
		return csv("caja", headers, filas);
	}

	private ResponseEntity<byte[]> csv(String nombre, List<String> headers, List<List<String>> filas) {
		String csv = CsvWriter.escribir(headers, filas);
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
		headersResp.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + ".csv\"");
		return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headersResp, HttpStatus.OK);
	}
}
