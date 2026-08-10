package com.sistema.stock.port.in;

import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;

public interface GestionarMerma {

	record RegistrarMermaCommand(Long itemId, Long loteId, BigDecimal cantidad, String motivo) {
	}

	MovimientoStock registrarMerma(RegistrarMermaCommand command);
}
