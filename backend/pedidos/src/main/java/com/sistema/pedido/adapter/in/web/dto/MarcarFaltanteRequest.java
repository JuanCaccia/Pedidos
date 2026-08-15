package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MarcarFaltanteRequest(@NotNull Long itemId, @NotNull @Positive BigDecimal cantidad, String motivo) {
}
