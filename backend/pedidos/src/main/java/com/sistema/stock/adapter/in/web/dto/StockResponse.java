package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Item;

import java.math.BigDecimal;

public record StockResponse(Long itemId, String sku, String itemNombre, BigDecimal disponible, BigDecimal reservasActivas) {

	public static StockResponse of(Item item, BigDecimal disponible, BigDecimal reservasActivas) {
		return new StockResponse(item.getId(), item.getSku(), item.getNombre(), disponible, reservasActivas);
	}
}
