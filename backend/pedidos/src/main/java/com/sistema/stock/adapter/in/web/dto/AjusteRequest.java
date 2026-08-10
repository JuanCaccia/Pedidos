package com.sistema.stock.adapter.in.web.dto;

import java.math.BigDecimal;

public record AjusteRequest(Long itemId, BigDecimal cantidad, String motivo) {
}
