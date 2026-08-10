package com.sistema.pedido.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

public record CrearPedidoRequest(Long clienteId, Long vendedorId, LocalDate fechaJornada, String observaciones,
		List<LineaRequest> items) {
}
