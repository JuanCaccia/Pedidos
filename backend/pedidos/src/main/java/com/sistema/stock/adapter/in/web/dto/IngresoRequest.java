package com.sistema.stock.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngresoRequest(Long itemId, String codigoLote, LocalDate fechaVencimiento, BigDecimal cantidad, String motivo) {
}
