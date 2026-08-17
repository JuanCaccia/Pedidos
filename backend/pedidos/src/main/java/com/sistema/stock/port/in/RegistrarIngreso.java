package com.sistema.stock.port.in;

import com.sistema.stock.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RegistrarIngreso {

	record CrearIngresoCommand(Long itemId, String codigoLote, LocalDate fechaVencimiento, BigDecimal cantidad, String motivo,
			Long proveedorId, BigDecimal precioUnitario) {
	}

	Lote crearIngreso(CrearIngresoCommand command);
}
