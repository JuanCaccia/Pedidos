package com.sistema.pedido.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConsolidarRequest(@NotNull @Size(min = 2) List<Long> pedidoIds) {
}
