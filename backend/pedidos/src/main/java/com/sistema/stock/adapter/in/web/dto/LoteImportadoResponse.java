package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteImportadoResponse(Long loteId, Long itemId, String codigoLote, BigDecimal cantidad,
		BigDecimal precioUnitario, LocalDate fechaVencimiento) {

	public static LoteImportadoResponse from(Lote lote) {
		return new LoteImportadoResponse(lote.getId(), lote.getItemId(), lote.getCodigoLote(),
				lote.getCantidadIngresada(), lote.getPrecioUnitario(), lote.getFechaVencimiento());
	}
}
