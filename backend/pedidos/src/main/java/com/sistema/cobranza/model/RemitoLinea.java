package com.sistema.cobranza.model;

import java.math.BigDecimal;

public class RemitoLinea {

	private Long id;
	private Long itemId;
	private BigDecimal cantidad;
	private BigDecimal precioUnitario;
	private BigDecimal subtotal;

	public RemitoLinea() {
	}

	public RemitoLinea(Long itemId, BigDecimal cantidad, BigDecimal precioUnitario) {
		this.itemId = itemId;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.subtotal = cantidad.multiply(precioUnitario);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}
}
