package com.sistema.compra.port.in;

import com.sistema.compra.model.OrdenCompra;
import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;
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

	record RecepcionCsvLineaCommand(Long itemId, BigDecimal cantidadRecibida, BigDecimal precioUnitario,
			LocalDate fechaVencimiento, String codigoLote) {
	}

	record RecepcionCsvCommand(Long ordenId, List<RecepcionCsvLineaCommand> lineas) {
	}

	OrdenCompra crearOrdenCompra(CrearOrdenCompraCommand command);

	OrdenCompra registrarRecepcion(RecepcionCommand command);

	List<Lote> registrarRecepcionCsv(RecepcionCsvCommand command);

	void cancelarOrdenCompra(Long ordenId);
}
