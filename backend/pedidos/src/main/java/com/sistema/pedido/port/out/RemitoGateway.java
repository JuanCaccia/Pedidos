package com.sistema.pedido.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface RemitoGateway {

	record LineaEntregada(Long itemId, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	long generarRemito(Long pedidoId, Long clienteId, List<LineaEntregada> lineas);
}
