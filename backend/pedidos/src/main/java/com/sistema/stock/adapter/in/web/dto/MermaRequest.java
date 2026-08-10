package com.sistema.stock.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MermaRequest(@NotNull Long itemId, @NotNull Long loteId, @NotNull @Positive BigDecimal cantidad,
		@NotBlank String motivo) {
}
