package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.cobranza.model.FormaPago;
import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobranza")
public class CobranzaJpaEntity extends BaseEntity {

	@Column(name = "cliente_id", nullable = false)
	private Long clienteId;

	@Column(name = "pedido_id")
	private Long pedidoId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pago", nullable = false, length = 20)
	private FormaPago formaPago;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Column(length = 255)
	private String observaciones;

	protected CobranzaJpaEntity() {
		// required by JPA
	}

	public CobranzaJpaEntity(Long clienteId, Long pedidoId, BigDecimal monto, FormaPago formaPago,
			LocalDateTime fecha, String observaciones) {
		this.clienteId = clienteId;
		this.pedidoId = pedidoId;
		this.monto = monto;
		this.formaPago = formaPago;
		this.fecha = fecha;
		this.observaciones = observaciones;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public void setMonto(BigDecimal monto) {
		this.monto = monto;
	}

	public FormaPago getFormaPago() {
		return formaPago;
	}

	public void setFormaPago(FormaPago formaPago) {
		this.formaPago = formaPago;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}
}
