package com.sistema.cobranza.adapter.in.web.dto;

import com.sistema.cobranza.model.Cobranza;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CobranzaResponse(Long id, Long clienteId, Long pedidoId, BigDecimal monto, String formaPago,
		LocalDateTime fecha, String observaciones) {

	public static CobranzaResponse from(Cobranza cobranza) {
		return new CobranzaResponse(cobranza.getId(), cobranza.getClienteId(), cobranza.getPedidoId(),
				cobranza.getMonto(), cobranza.getFormaPago().name(), cobranza.getFecha(), cobranza.getObservaciones());
	}
}
