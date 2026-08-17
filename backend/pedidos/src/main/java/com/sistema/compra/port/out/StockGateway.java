package com.sistema.compra.port.out;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface StockGateway {

	boolean existeItem(Long itemId);

	boolean itemActivo(Long itemId);

	void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo, Long proveedorId,
			BigDecimal precioUnitario);

	Lote registrarIngresoConLote(Long itemId, String codigoLote, LocalDate fechaVencimiento, BigDecimal cantidad,
			String motivo, Long proveedorId, BigDecimal precioUnitario);
}
