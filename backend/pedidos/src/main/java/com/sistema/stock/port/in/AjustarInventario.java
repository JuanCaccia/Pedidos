package com.sistema.stock.port.in;

import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;

public interface AjustarInventario {

	record AjusteInventarioCommand(Long itemId, BigDecimal cantidad, String motivo, com.sistema.usuario.model.Usuario actor) {
	}

	MovimientoStock ajustarInventario(AjusteInventarioCommand command);
}
