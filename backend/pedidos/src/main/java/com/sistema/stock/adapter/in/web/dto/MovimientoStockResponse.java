package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoStockResponse(Long id, String tipo, Long itemId, Long loteId, Long pedidoId,
		BigDecimal cantidad, LocalDateTime fecha, String motivo) {

	public static MovimientoStockResponse from(MovimientoStock m) {
		return new MovimientoStockResponse(m.getId(), m.getTipo().name(), m.getItemId(), m.getLoteId(),
				m.getPedidoId(), m.getCantidad(), m.getFecha(), m.getMotivo());
	}
}
