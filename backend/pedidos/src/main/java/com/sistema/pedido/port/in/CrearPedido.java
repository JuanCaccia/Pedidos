package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CrearPedido {

	record LineaPedidoCommand(Long itemId, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	record CrearPedidoCommand(Long clienteId, Long vendedorId, LocalDate fechaJornada, String observaciones,
			List<LineaPedidoCommand> items) {
	}

	Pedido crearPedido(CrearPedidoCommand command);
}
