package com.sistema.sustitucion.adapter.in.web.dto;

import java.math.BigDecimal;

public record SustituirRequest(Long pedidoId, Long itemOriginalId, Long itemSustitutoId, BigDecimal cantidad,
		String observaciones) {
}
