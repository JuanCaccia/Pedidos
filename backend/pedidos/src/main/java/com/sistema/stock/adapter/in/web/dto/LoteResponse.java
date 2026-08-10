package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(Long id, Long itemId, String codigoLote, LocalDate fechaIngreso, LocalDate fechaVencimiento,
		BigDecimal cantidadIngresada) {

	public static LoteResponse from(Lote lote) {
		return new LoteResponse(lote.getId(), lote.getItemId(), lote.getCodigoLote(), lote.getFechaIngreso(),
				lote.getFechaVencimiento(), lote.getCantidadIngresada());
	}
}
