package com.sistema.stock.adapter.in.web.dto;

import java.math.BigDecimal;

public record MermaRequest(Long itemId, Long loteId, BigDecimal cantidad, String motivo) {
}
