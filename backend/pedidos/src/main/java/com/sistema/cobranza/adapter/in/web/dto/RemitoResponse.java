package com.sistema.cobranza.adapter.in.web.dto;

import com.sistema.cobranza.model.Remito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RemitoResponse(Long id, String numero, Long pedidoId, Long clienteId, LocalDateTime fechaEmision,
		BigDecimal montoTotal, List<RemitoLineaResponse> lineas) {

	public static RemitoResponse from(Remito remito) {
		return new RemitoResponse(remito.getId(), remito.getNumero(), remito.getPedidoId(), remito.getClienteId(),
				remito.getFechaEmision(), remito.getMontoTotal(),
				remito.getLineas().stream().map(RemitoLineaResponse::from).toList());
	}
}
