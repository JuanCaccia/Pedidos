package com.sistema.reporte.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConsultarReportes {

	record ItemStockReporte(Long itemId, String sku, String nombre, BigDecimal disponible, BigDecimal reservasActivas) {
	}

	record VentaVendedorReporte(Long vendedorId, String vendedorNombre, long cantidadPedidos,
			BigDecimal cantidadUnidades, BigDecimal monto) {
	}

	record RutaReporte(Long rutaId, Long zonaId, Long repartidorId, LocalDate fechaJornada, String estado,
			int cantidadPedidos) {
	}

	List<ItemStockReporte> stockGeneral();

	List<VentaVendedorReporte> ventasPorVendedor(Long vendedorId, LocalDate desde, LocalDate hasta);

	List<RutaReporte> rutasPorFecha(LocalDate fecha);
}
