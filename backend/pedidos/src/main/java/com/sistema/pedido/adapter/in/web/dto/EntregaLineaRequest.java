package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record EntregaLineaRequest(@NotNull Long pedidoItemId, @NotNull @PositiveOrZero BigDecimal cantidadEntregada) {
}
