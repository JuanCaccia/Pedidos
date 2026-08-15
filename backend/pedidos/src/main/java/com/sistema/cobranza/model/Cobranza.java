package com.sistema.cobranza.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Cobranza {

	private Long id;
	private Long clienteId;
	private Long pedidoId;
	private BigDecimal monto;
	private FormaPago formaPago;
	private LocalDateTime fecha;
	private String observaciones;

	public Cobranza() {
	}

	public Cobranza(Long clienteId, Long pedidoId, BigDecimal monto, FormaPago formaPago, LocalDateTime fecha, String observaciones) {
		this.clienteId = clienteId;
		this.pedidoId = pedidoId;
		this.monto = monto;
		this.formaPago = formaPago;
		this.fecha = fecha;
		this.observaciones = observaciones;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
