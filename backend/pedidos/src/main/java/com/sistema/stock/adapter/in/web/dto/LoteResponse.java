package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(Long id, Long itemId, String codigoLote, LocalDate fechaIngreso, LocalDate fechaVencimiento,
		BigDecimal cantidadIngresada, BigDecimal disponible, String estado, String itemNombre, String itemSku) {

	public static LoteResponse from(Lote lote, BigDecimal disponible, String itemNombre, String itemSku) {
		return new LoteResponse(lote.getId(), lote.getItemId(), lote.getCodigoLote(), lote.getFechaIngreso(),
				lote.getFechaVencimiento(), lote.getCantidadIngresada(), disponible,
				derivarEstado(lote.getFechaVencimiento(), disponible), itemNombre, itemSku);
	}

	public static String derivarEstado(LocalDate fechaVencimiento, BigDecimal disponible) {
		if (fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now())) {
			return "VENCIDO";
		}
		if (disponible != null && disponible.signum() <= 0) {
			return "AGOTADO";
		}
		return "VIGENTE";
	}
}
