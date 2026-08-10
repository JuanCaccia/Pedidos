package com.sistema.ruta.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record AsignarPedidosRequest(@NotEmpty List<Long> pedidoIds) {
}
