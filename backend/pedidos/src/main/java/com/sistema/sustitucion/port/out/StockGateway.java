package com.sistema.sustitucion.port.out;

import java.math.BigDecimal;

public interface StockGateway {

	void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo);

	void egresar(Long itemId, Long pedidoId, BigDecimal cantidad);

	BigDecimal consultarPrecioLista(Long itemId);
}
