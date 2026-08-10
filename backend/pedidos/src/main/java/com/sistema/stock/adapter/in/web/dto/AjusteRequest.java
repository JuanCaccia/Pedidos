package com.sistema.stock.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AjusteRequest(@NotNull Long itemId, @NotNull BigDecimal cantidad, @NotBlank String motivo) {
}
