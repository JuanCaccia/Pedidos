package com.sistema.sustitucion.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sustitucion {

	private Long id;
	private Long pedidoId;
	private Long itemOriginalId;
	private Long itemSustitutoId;
	private BigDecimal cantidad;
	private BigDecimal diferenciaPrecio;
	private LocalDateTime fecha;
	private String observaciones;

	public Sustitucion() {
	}

	public Sustitucion(Long pedidoId, Long itemOriginalId, Long itemSustitutoId, BigDecimal cantidad,
			BigDecimal diferenciaPrecio, LocalDateTime fecha, String observaciones) {
		this.pedidoId = pedidoId;
		this.itemOriginalId = itemOriginalId;
		this.itemSustitutoId = itemSustitutoId;
		this.cantidad = cantidad;
		this.diferenciaPrecio = diferenciaPrecio;
		this.fecha = fecha;
		this.observaciones = observaciones;
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

	public Long getItemOriginalId() {
		return itemOriginalId;
	}

	public void setItemOriginalId(Long itemOriginalId) {
		this.itemOriginalId = itemOriginalId;
	}

	public Long getItemSustitutoId() {
		return itemSustitutoId;
	}

	public void setItemSustitutoId(Long itemSustitutoId) {
		this.itemSustitutoId = itemSustitutoId;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal getDiferenciaPrecio() {
		return diferenciaPrecio;
	}

	public void setDiferenciaPrecio(BigDecimal diferenciaPrecio) {
		this.diferenciaPrecio = diferenciaPrecio;
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
