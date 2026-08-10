package com.sistema.pedido.adapter.in.web.dto;

import java.math.BigDecimal;

public record EntregaLineaRequest(Long pedidoItemId, BigDecimal cantidadEntregada) {
}
