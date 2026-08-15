package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EntregaRequest(@NotEmpty @Valid List<EntregaLineaRequest> entregas) {
}
