package com.sistema.ruta.model;

public class RutaPedido {

	private Long id;
	private Long pedidoId;

	public RutaPedido() {
	}

	public RutaPedido(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}
}
