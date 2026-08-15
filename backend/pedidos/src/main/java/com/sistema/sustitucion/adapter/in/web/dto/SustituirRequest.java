package com.sistema.sustitucion.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SustituirRequest(@NotNull Long pedidoId, @NotNull Long itemOriginalId, @NotNull Long itemSustitutoId,
		@NotNull @Positive BigDecimal cantidad, String observaciones) {
}
