package com.sistema.pedido.port.in;

import com.sistema.pedido.model.Pedido;

public interface GestionarLogisticaPedido {

	Pedido asignarARuta(Long pedidoId);

	Pedido iniciarViaje(Long pedidoId);
}
