package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecepcionLineaRequest(@NotNull Long lineaId, @NotNull @Positive BigDecimal cantidadRecibida) {
}
