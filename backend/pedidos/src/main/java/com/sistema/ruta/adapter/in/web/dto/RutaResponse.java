package com.sistema.ruta.adapter.in.web.dto;

import com.sistema.ruta.model.Ruta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RutaResponse(Long id, Long zonaId, Long repartidorId, LocalDate fechaJornada, BigDecimal capacidadBultos,
		String estado, List<Long> pedidoIds) {

	public static RutaResponse from(Ruta ruta) {
		return new RutaResponse(ruta.getId(), ruta.getZonaId(), ruta.getRepartidorId(), ruta.getFechaJornada(),
				ruta.getCapacidadBultos(), ruta.getEstado().name(), ruta.getPedidoIds());
	}
}
