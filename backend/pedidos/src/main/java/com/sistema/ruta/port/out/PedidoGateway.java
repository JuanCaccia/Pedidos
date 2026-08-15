package com.sistema.ruta.port.out;

public interface PedidoGateway {

	java.math.BigDecimal unidadesDe(Long pedidoId);

	String numeroDePedido(Long pedidoId);

	boolean existePedido(Long pedidoId);

	boolean estaDisponibleParaRuta(Long pedidoId);

	boolean clientePerteneceAZona(Long pedidoId, Long zonaId);

	void asignarARuta(Long pedidoId);

	void iniciarViaje(Long pedidoId);

	boolean estaEnViaje(Long pedidoId);
}
