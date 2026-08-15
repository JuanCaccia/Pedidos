package com.sistema.sustitucion.port.in;

import com.sistema.sustitucion.model.Sustitucion;

import java.math.BigDecimal;

public interface RegistrarSustitucion {

	record SustituirCommand(Long pedidoId, Long itemOriginalId, Long itemSustitutoId, BigDecimal cantidad,
			String observaciones, com.sistema.usuario.model.Usuario actor) {
	}

	Sustitucion sustituir(SustituirCommand command);
}
