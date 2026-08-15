package com.sistema.compra.model;

import java.math.BigDecimal;

public class OrdenCompraLinea {

	private Long id;
	private Long itemId;
	private BigDecimal cantidadPedida;
	private BigDecimal cantidadRecibida = BigDecimal.ZERO;
	private BigDecimal precioUnitario;

	public OrdenCompraLinea() {
	}

	public OrdenCompraLinea(Long itemId, BigDecimal cantidadPedida, BigDecimal precioUnitario) {
		this.itemId = itemId;
		this.cantidadPedida = cantidadPedida;
		this.precioUnitario = precioUnitario;
	}

	public void recibir(BigDecimal cantidad) {
		this.cantidadRecibida = this.cantidadRecibida.add(cantidad);
	}

	public BigDecimal restante() {
		return this.cantidadPedida.subtract(this.cantidadRecibida);
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

	public BigDecimal getCantidadPedida() {
		return cantidadPedida;
	}

	public void setCantidadPedida(BigDecimal cantidadPedida) {
		this.cantidadPedida = cantidadPedida;
	}

	public BigDecimal getCantidadRecibida() {
		return cantidadRecibida;
	}

	public void setCantidadRecibida(BigDecimal cantidadRecibida) {
		this.cantidadRecibida = cantidadRecibida;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = precioUnitario;
	}
}
