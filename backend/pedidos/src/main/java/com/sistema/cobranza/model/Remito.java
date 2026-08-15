package com.sistema.cobranza.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Remito {

	private Long id;
	private String numero;
	private Long pedidoId;
	private Long clienteId;
	private LocalDateTime fechaEmision;
	private BigDecimal montoTotal = BigDecimal.ZERO;
	private List<RemitoLinea> lineas = new ArrayList<>();

	public Remito() {
	}

	public Remito(Long pedidoId, Long clienteId) {
		this.pedidoId = pedidoId;
		this.clienteId = clienteId;
		this.fechaEmision = LocalDateTime.now();
	}

	public void agregarLinea(RemitoLinea linea) {
		this.lineas.add(linea);
		this.montoTotal = this.montoTotal.add(linea.getSubtotal());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public List<RemitoLinea> getLineas() {
		return lineas;
	}

	public void setLineas(List<RemitoLinea> lineas) {
		this.lineas = lineas;
	}
}
