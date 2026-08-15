package com.sistema.pedido.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface StockGateway {

	boolean existeItem(Long itemId);

	BigDecimal consultarDisponible(Long itemId);

	void reservar(Long itemId, Long pedidoId, BigDecimal cantidad);

	void liberarReserva(Long itemId, Long pedidoId, BigDecimal cantidad);

	void egresar(Long itemId, Long pedidoId, BigDecimal cantidad);

	List<Long> listarLoteIdsDisponibles(Long itemId);

	void registrarMerma(Long itemId, Long loteId, BigDecimal cantidad, String motivo);

	void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo);

	BigDecimal consultarPrecioLista(Long itemId);
}
