package com.sistema.compra.port.out;

import java.math.BigDecimal;

public interface StockGateway {

	boolean existeItem(Long itemId);

	boolean itemActivo(Long itemId);

	void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo, Long proveedorId,
			BigDecimal precioUnitario);
}
