package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearOrdenCompraRequest(@NotNull Long proveedorId, String observaciones,
		@NotEmpty List<@Valid LineaOrdenRequest> lineas) {
}
