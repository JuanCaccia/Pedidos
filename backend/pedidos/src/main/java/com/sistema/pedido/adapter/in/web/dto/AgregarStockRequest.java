package com.sistema.pedido.adapter.in.web.dto;

import java.math.BigDecimal;

public record AgregarStockRequest(Long itemId, BigDecimal cantidad) {
}
