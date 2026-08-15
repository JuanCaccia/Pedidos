package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "remito")
public class RemitoJpaEntity extends BaseEntity {

	@Column(nullable = false, unique = true, length = 20)
	private String numero;

	@Column(name = "pedido_id", nullable = false)
	private Long pedidoId;

	@Column(name = "cliente_id", nullable = false)
	private Long clienteId;

	@Column(name = "fecha_emision", nullable = false)
	private LocalDateTime fechaEmision;

	@Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal montoTotal;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "remito_id", nullable = false)
	private List<RemitoLineaJpaEntity> lineas = new ArrayList<>();

	protected RemitoJpaEntity() {
		// required by JPA
	}

	public RemitoJpaEntity(String numero, Long pedidoId, Long clienteId, LocalDateTime fechaEmision,
			BigDecimal montoTotal) {
		this.numero = numero;
		this.pedidoId = pedidoId;
		this.clienteId = clienteId;
		this.fechaEmision = fechaEmision;
		this.montoTotal = montoTotal;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public LocalDateTime getFechaEmision() {
		return fechaEmision;
	}

	public void setFechaEmision(LocalDateTime fechaEmision) {
		this.fechaEmision = fechaEmision;
	}

	public BigDecimal getMontoTotal() {
		return montoTotal;
	}

	public void setMontoTotal(BigDecimal montoTotal) {
		this.montoTotal = montoTotal;
	}

	public List<RemitoLineaJpaEntity> getLineas() {
		return lineas;
	}

	public void setLineas(List<RemitoLineaJpaEntity> lineas) {
		this.lineas = lineas;
	}
}
