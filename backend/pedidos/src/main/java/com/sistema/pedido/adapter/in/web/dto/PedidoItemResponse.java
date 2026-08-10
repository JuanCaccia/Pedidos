package com.sistema.pedido.adapter.in.web.dto;

import com.sistema.pedido.model.PedidoItem;

import java.math.BigDecimal;

public record PedidoItemResponse(Long pedidoItemId, Long itemId, BigDecimal cantidadPedida,
		BigDecimal cantidadReservada, BigDecimal cantidadEntregada, BigDecimal precioUnitario, boolean pendienteStock) {

	public static PedidoItemResponse from(PedidoItem item) {
		return new PedidoItemResponse(item.getId(), item.getItemId(), item.getCantidadPedida(),
				item.getCantidadReservada(), item.getCantidadEntregada(), item.getPrecioUnitario(),
				item.isPendienteStock());
	}
}
