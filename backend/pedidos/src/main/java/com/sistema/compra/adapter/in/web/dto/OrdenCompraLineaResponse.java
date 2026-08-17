package com.sistema.compra.adapter.in.web.dto;

import com.sistema.compra.model.OrdenCompraLinea;

import java.math.BigDecimal;

public record OrdenCompraLineaResponse(Long id, Long itemId, BigDecimal cantidadPedida, BigDecimal cantidadRecibida,
		BigDecimal restante) {

	public static OrdenCompraLineaResponse from(OrdenCompraLinea linea) {
		return new OrdenCompraLineaResponse(linea.getId(), linea.getItemId(), linea.getCantidadPedida(),
				linea.getCantidadRecibida(), linea.restante());
	}
}
