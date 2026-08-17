package com.sistema.compra.port.in;

import com.sistema.compra.model.OrdenCompra;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarOrdenCompra {

	record LineaOrdenCommand(Long itemId, BigDecimal cantidad) {
	}

	record CrearOrdenCompraCommand(Long proveedorId, String observaciones, List<LineaOrdenCommand> lineas) {
	}

	record RecepcionLineaCommand(Long lineaId, BigDecimal cantidadRecibida, BigDecimal precioUnitario) {
	}

	record RecepcionCommand(Long ordenId, List<RecepcionLineaCommand> lineas) {
	}

	OrdenCompra crearOrdenCompra(CrearOrdenCompraCommand command);

	OrdenCompra registrarRecepcion(RecepcionCommand command);

	void cancelarOrdenCompra(Long ordenId);
}
