package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LineaRequest(@NotNull Long itemId, @NotNull @Positive BigDecimal cantidad,
		@NotNull @PositiveOrZero BigDecimal precioUnitario) {
}
