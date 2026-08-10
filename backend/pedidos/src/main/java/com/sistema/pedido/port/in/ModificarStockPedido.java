package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

import java.math.BigDecimal;

public interface ModificarStockPedido {

	Pedido agregarUnidades(Long pedidoId, Long itemId, BigDecimal cantidad);
}
