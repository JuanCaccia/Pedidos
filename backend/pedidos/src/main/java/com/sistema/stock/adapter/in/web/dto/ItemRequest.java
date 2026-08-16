package com.sistema.stock.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemRequest(@NotBlank String sku, @NotBlank String nombre, @NotBlank String unidadMedida,
		BigDecimal stockMinimo, BigDecimal precioLista, Long categoriaId) {
}
