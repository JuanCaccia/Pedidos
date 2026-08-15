package com.sistema.compra.adapter.in.web.dto;

import com.sistema.compra.model.OrdenCompra;

import java.time.LocalDateTime;
import java.util.List;

public record OrdenCompraResponse(Long id, String numero, Long proveedorId, LocalDateTime fecha, String estado,
		String observaciones, List<OrdenCompraLineaResponse> lineas) {

	public static OrdenCompraResponse from(OrdenCompra orden) {
		List<OrdenCompraLineaResponse> lineas = orden.getLineas().stream().map(OrdenCompraLineaResponse::from).toList();
		return new OrdenCompraResponse(orden.getId(), orden.getNumero(), orden.getProveedorId(), orden.getFecha(),
				orden.getEstado().name(), orden.getObservaciones(), lineas);
	}
}
