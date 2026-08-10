package com.sistema.stock.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IngresoRequest(@NotNull Long itemId, String codigoLote, LocalDate fechaVencimiento,
		@NotNull @Positive BigDecimal cantidad, String motivo) {
}
