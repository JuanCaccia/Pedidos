package com.sistema.pedido.adapter.in.web.dto;

import com.sistema.pedido.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Long id, String numero, Long clienteId, Long vendedorId, Long pedidoPadreId, String estado,
		LocalDateTime fechaCreacion, LocalDateTime updatedAt, LocalDate fechaJornada, String observaciones, BigDecimal total,
		boolean express, List<PedidoItemResponse> items) {

	public static PedidoResponse from(Pedido pedido) {
		List<PedidoItemResponse> items = pedido.getItems().stream().map(PedidoItemResponse::from).toList();
		return new PedidoResponse(pedido.getId(), pedido.getNumero(), pedido.getClienteId(), pedido.getVendedorId(),
				pedido.getPedidoPadreId(), pedido.getEstado().name(), pedido.getFechaCreacion(),
				pedido.getUpdatedAt(), pedido.getFechaJornada(), pedido.getObservaciones(), pedido.getTotal(),
				pedido.isExpress(), items);
	}
}
