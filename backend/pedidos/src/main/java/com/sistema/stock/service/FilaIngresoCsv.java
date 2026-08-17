package com.sistema.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FilaIngresoCsv(int numeroFila, String sku, BigDecimal cantidad, BigDecimal precioUnitario,
		LocalDate fechaVencimiento, String codigoLote) {
}
