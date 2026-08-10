package com.sistema.stock.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoStock {

	private Long id;
	private TipoMovimiento tipo;
	private Long itemId;
	private Long loteId;
	private Long pedidoId;
	private BigDecimal cantidad;
	private LocalDateTime fecha;
	private String motivo;

	public MovimientoStock() {
	}

	public MovimientoStock(TipoMovimiento tipo, Long itemId, Long loteId, Long pedidoId,
			BigDecimal cantidad, LocalDateTime fecha, String motivo) {
		this.tipo = tipo;
		this.itemId = itemId;
		this.loteId = loteId;
		this.pedidoId = pedidoId;
		this.cantidad = cantidad;
		this.fecha = fecha;
		this.motivo = motivo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public void setTipo(TipoMovimiento tipo) {
		this.tipo = tipo;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public Long getLoteId() {
		return loteId;
	}

	public void setLoteId(Long loteId) {
		this.loteId = loteId;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
