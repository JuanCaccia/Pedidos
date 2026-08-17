package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

public interface ModificarStockPedido {

	Pedido agregarUnidades(Long pedidoId, Long itemId, BigDecimal cantidad);

	Pedido reintentarStock(Long pedidoId);

	Pedido marcarFaltante(MarcarFaltanteCommand command);

	Pedido consolidarPedidos(ConsolidarCommand command);

	record MarcarFaltanteCommand(Long pedidoId, Long itemId, BigDecimal cantidad, String motivo,
			com.sistema.usuario.model.Usuario actor) {
	}

	record ConsolidarCommand(List<Long> pedidoIds, Long vendedorId) {
	}
}
