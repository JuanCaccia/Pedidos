package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LineaOrdenRequest(@NotNull Long itemId, @NotNull @Positive BigDecimal cantidad) {
}
