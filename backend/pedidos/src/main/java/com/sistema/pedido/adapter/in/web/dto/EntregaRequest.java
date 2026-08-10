package com.sistema.pedido.adapter.in.web.dto;

import java.util.List;

public record EntregaRequest(List<EntregaLineaRequest> entregas) {
}
