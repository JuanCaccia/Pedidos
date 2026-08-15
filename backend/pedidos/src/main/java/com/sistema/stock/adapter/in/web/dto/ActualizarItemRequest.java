package com.sistema.stock.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ActualizarItemRequest(@NotBlank String nombre, @NotBlank String unidadMedida, BigDecimal stockMinimo,
		BigDecimal precioLista, String categoria) {
}
