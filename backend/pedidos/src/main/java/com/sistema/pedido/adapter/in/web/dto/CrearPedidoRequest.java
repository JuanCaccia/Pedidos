package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CrearPedidoRequest(@NotNull Long clienteId, @NotNull Long vendedorId, LocalDate fechaJornada,
		String observaciones, Boolean express, @NotEmpty @Valid List<LineaRequest> items) {
}
