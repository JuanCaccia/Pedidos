package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Item;

public record ItemResponse(Long id, String sku, String nombre, String unidadMedida, boolean activo) {

	public static ItemResponse from(Item item) {
		return new ItemResponse(item.getId(), item.getSku(), item.getNombre(), item.getUnidadMedida(), item.isActivo());
	}
}
