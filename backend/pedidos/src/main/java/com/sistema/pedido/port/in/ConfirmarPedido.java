package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

public interface ConfirmarPedido {

	Pedido confirmarPedido(Long pedidoId);
}
