package com.sistema.stock.port.in;

import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;

public interface AjustarInventario {

	record AjusteInventarioCommand(Long itemId, BigDecimal cantidad, String motivo) {
	}

	MovimientoStock ajustarInventario(AjusteInventarioCommand command);
}
