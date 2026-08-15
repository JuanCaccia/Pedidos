package com.sistema.cobranza.adapter.in.web.dto;

import com.sistema.cobranza.model.RemitoLinea;

import java.math.BigDecimal;

public record RemitoLineaResponse(Long id, Long itemId, BigDecimal cantidad, BigDecimal precioUnitario,
		BigDecimal subtotal) {

	public static RemitoLineaResponse from(RemitoLinea linea) {
		return new RemitoLineaResponse(linea.getId(), linea.getItemId(), linea.getCantidad(),
				linea.getPrecioUnitario(), linea.getSubtotal());
	}
}
