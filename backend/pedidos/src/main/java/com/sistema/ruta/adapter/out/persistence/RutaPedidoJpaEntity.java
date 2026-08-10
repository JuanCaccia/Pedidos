package com.sistema.ruta.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ruta_pedido")
public class RutaPedidoJpaEntity extends BaseEntity {

	@Column(name = "pedido_id", nullable = false)
	private Long pedidoId;

	protected RutaPedidoJpaEntity() {
		// required by JPA
	}

	public RutaPedidoJpaEntity(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}
}
