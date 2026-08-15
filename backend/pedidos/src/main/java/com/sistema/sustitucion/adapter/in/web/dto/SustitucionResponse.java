package com.sistema.sustitucion.adapter.in.web.dto;

import com.sistema.sustitucion.model.Sustitucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SustitucionResponse(Long id, Long pedidoId, Long itemOriginalId, Long itemSustitutoId,
		BigDecimal cantidad, BigDecimal diferenciaPrecio, LocalDateTime fecha, String observaciones) {

	public static SustitucionResponse from(Sustitucion s) {
		return new SustitucionResponse(s.getId(), s.getPedidoId(), s.getItemOriginalId(), s.getItemSustitutoId(),
				s.getCantidad(), s.getDiferenciaPrecio(), s.getFecha(), s.getObservaciones());
	}
}
