package com.sistema.sustitucion.port.out;

import java.math.BigDecimal;

public interface StockGateway {

	void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo);

	BigDecimal consultarPrecioLista(Long itemId);
}
