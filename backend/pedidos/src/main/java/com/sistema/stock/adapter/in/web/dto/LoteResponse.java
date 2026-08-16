package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;
import com.sistema.stock.model.LoteEstado;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(Long id, Long itemId, Long proveedorId, String codigoLote, LocalDate fechaIngreso,
		LocalDate fechaVencimiento, BigDecimal cantidadIngresada, BigDecimal disponible, String estado, String itemNombre,
		String itemSku) {

	public static LoteResponse from(Lote lote, BigDecimal disponible, String itemNombre, String itemSku) {
		return new LoteResponse(lote.getId(), lote.getItemId(), lote.getProveedorId(), lote.getCodigoLote(),
				lote.getFechaIngreso(), lote.getFechaVencimiento(), lote.getCantidadIngresada(), disponible,
				resolverEstado(lote, disponible), itemNombre, itemSku);
	}

	// Resolución del estado: DESCARTADO (persistido, explícito) tiene prioridad; el resto se deriva.
	// VENCIDO se deriva por fecha de vencimiento (siempre vigente); AGOTADO por saldo o por estado persistido.
	private static String resolverEstado(Lote lote, BigDecimal disponible) {
		if (lote.getEstado() == LoteEstado.DESCARTADO) {
			return "DESCARTADO";
		}
		String derivado = derivarEstado(lote.getFechaVencimiento(), disponible);
		if ("VIGENTE".equals(derivado) && lote.getEstado() == LoteEstado.AGOTADO) {
			return "AGOTADO";
		}
		return derivado;
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
