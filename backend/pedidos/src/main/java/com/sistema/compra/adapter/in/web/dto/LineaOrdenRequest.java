package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record LineaOrdenRequest(@NotNull Long itemId, @NotNull @Positive BigDecimal cantidad,
		@NotNull @PositiveOrZero BigDecimal precioUnitario) {
}
