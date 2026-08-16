package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Item;

import java.math.BigDecimal;

public record ItemResponse(Long id, String sku, String nombre, String unidadMedida, BigDecimal stockMinimo,
		BigDecimal precioLista, Long categoriaId, String categoriaNombre, boolean activo) {

	public static ItemResponse from(Item item) {
		return new ItemResponse(item.getId(), item.getSku(), item.getNombre(), item.getUnidadMedida(),
				item.getStockMinimo(), item.getPrecioLista(), item.getCategoriaId(), item.getCategoriaNombre(), item.isActivo());
	}
}
