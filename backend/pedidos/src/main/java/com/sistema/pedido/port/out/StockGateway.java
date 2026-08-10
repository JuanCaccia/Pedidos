package com.sistema.pedido.port.out;

import java.math.BigDecimal;

public interface StockGateway {

	boolean existeItem(Long itemId);

	BigDecimal consultarDisponible(Long itemId);

	void reservar(Long itemId, Long pedidoId, BigDecimal cantidad);

	void liberarReserva(Long itemId, Long pedidoId, BigDecimal cantidad);

	void egresar(Long itemId, Long pedidoId, BigDecimal cantidad);
}
