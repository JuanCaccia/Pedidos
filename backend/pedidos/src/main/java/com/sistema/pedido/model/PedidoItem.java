package com.sistema.pedido.model;

import java.math.BigDecimal;

public class PedidoItem {

	private Long id;
	private Long itemId;
	private BigDecimal cantidadPedida;
	private BigDecimal cantidadReservada = BigDecimal.ZERO;
	private BigDecimal cantidadEntregada = BigDecimal.ZERO;
	private BigDecimal precioUnitario;
	private boolean pendienteStock;

	public PedidoItem() {
	}

	public PedidoItem(Long itemId, BigDecimal cantidadPedida, BigDecimal precioUnitario) {
		this.itemId = itemId;
		this.cantidadPedida = cantidadPedida;
		this.precioUnitario = precioUnitario;
	}

	public void reservar(BigDecimal cantidad) {
		this.cantidadReservada = this.cantidadReservada.add(cantidad);
	}

	public void agregarStock(BigDecimal cantidad) {
		reservar(cantidad);
		if (this.cantidadReservada.compareTo(this.cantidadPedida) >= 0) {
			this.pendienteStock = false;
		}
	}

	public void marcarPendienteStock() {
		this.pendienteStock = true;
	}

	public void registrarEntrega(BigDecimal cantidad) {
		this.cantidadEntregada = this.cantidadEntregada.add(cantidad);
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

	public BigDecimal getCantidadReservada() {
		return cantidadReservada;
	}

	public void setCantidadReservada(BigDecimal cantidadReservada) {
		this.cantidadReservada = cantidadReservada;
	}

	public BigDecimal getCantidadEntregada() {
		return cantidadEntregada;
	}

	public void setCantidadEntregada(BigDecimal cantidadEntregada) {
		this.cantidadEntregada = cantidadEntregada;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public boolean isPendienteStock() {
		return pendienteStock;
	}

	public void setPendienteStock(boolean pendienteStock) {
		this.pendienteStock = pendienteStock;
	}
}
