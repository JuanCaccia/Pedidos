package com.sistema.reporte.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ConsultarReportes {

	record PorFormaPago(String formaPago, BigDecimal monto, long cantidad) {
	}

	record PorDia(LocalDate fecha, BigDecimal monto, long cantidad) {
	}

	record PorVendedor(Long vendedorId, String vendedorNombre, BigDecimal monto, long cantidad) {
	}

	record ResumenCaja(BigDecimal totalCobrado, long cantidadCobranzas,
			List<PorFormaPago> porFormaPago, List<PorDia> porDia, List<PorVendedor> porVendedor) {
	}

	record ItemStockReporte(Long itemId, String sku, String nombre, BigDecimal disponible, BigDecimal reservasActivas) {
	}

	record VentaVendedorReporte(Long vendedorId, String vendedorNombre, long cantidadPedidos,
			BigDecimal cantidadUnidades, BigDecimal monto) {
	}

	record RutaReporte(Long rutaId, Long zonaId, Long repartidorId, LocalDate fechaJornada, String estado,
			int cantidadPedidos) {
	}

	record CobranzaExport(LocalDateTime fecha, Long clienteId, String clienteNombre, Long pedidoId, String pedidoNumero,
			BigDecimal monto, String formaPago, String observaciones, BigDecimal saldo) {
	}

	List<ItemStockReporte> stockGeneral();

	List<VentaVendedorReporte> ventasPorVendedor(Long vendedorId, LocalDate desde, LocalDate hasta);

	List<RutaReporte> rutasPorFecha(LocalDate fecha);

	ResumenCaja resumenCaja(LocalDate desde, LocalDate hasta);

	List<CobranzaExport> cobranzasExport(Long clienteId, LocalDate desde, LocalDate hasta);
}
