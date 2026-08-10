package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngresoResponse(Long loteId, Long itemId, String codigoLote, LocalDate fechaIngreso, BigDecimal cantidad) {

	public static IngresoResponse from(Lote lote) {
		return new IngresoResponse(lote.getId(), lote.getItemId(), lote.getCodigoLote(),
				lote.getFechaIngreso(), lote.getCantidadIngresada());
	}
}
