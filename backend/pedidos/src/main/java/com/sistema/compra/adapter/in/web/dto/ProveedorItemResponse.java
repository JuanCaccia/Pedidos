package com.sistema.compra.adapter.in.web.dto;

import com.sistema.compra.model.ProveedorItem;

public record ProveedorItemResponse(Long proveedorId, Long itemId, String itemSku, String itemNombre,
		boolean activo) {

	public static ProveedorItemResponse from(ProveedorItem proveedorItem) {
		return new ProveedorItemResponse(proveedorItem.getProveedorId(), proveedorItem.getItemId(),
				proveedorItem.getItemSku(), proveedorItem.getItemNombre(), proveedorItem.isActivo());
	}
}
