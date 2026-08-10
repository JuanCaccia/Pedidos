package com.sistema.ruta.port.out;

public interface PedidoGateway {

	boolean existePedido(Long pedidoId);

	boolean estaDisponibleParaRuta(Long pedidoId);

	boolean clientePerteneceAZona(Long pedidoId, Long zonaId);

	void asignarARuta(Long pedidoId);

	void iniciarViaje(Long pedidoId);
}
