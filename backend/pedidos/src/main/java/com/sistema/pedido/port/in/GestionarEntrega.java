package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarEntrega {

	record EntregaLineaCommand(Long pedidoItemId, BigDecimal cantidadEntregada) {
	}

	record RegistrarEntregaCommand(Long pedidoId, List<EntregaLineaCommand> entregas) {
	}

	Pedido registrarEntrega(RegistrarEntregaCommand command);
}
