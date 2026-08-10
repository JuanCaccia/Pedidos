package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

public interface ReAgendarPedido {

	Pedido reAgendar(Long pedidoId);
}
