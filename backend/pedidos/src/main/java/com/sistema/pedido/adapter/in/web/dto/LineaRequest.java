package com.sistema.pedido.adapter.in.web.dto;

import java.math.BigDecimal;

public record LineaRequest(Long itemId, BigDecimal cantidad, BigDecimal precioUnitario) {
}
