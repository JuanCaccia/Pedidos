package com.sistema.sustitucion.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sustitucion")
public class SustitucionJpaEntity extends BaseEntity {

	@Column(name = "pedido_id", nullable = false)
	private Long pedidoId;

	@Column(name = "item_original_id", nullable = false)
	private Long itemOriginalId;

	@Column(name = "item_sustituto_id", nullable = false)
	private Long itemSustitutoId;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidad;

	@Column(name = "diferencia_precio", nullable = false, precision = 12, scale = 2)
	private BigDecimal diferenciaPrecio;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Column(length = 255)
	private String observaciones;

	protected SustitucionJpaEntity() {
		// required by JPA
	}

	public SustitucionJpaEntity(Long pedidoId, Long itemOriginalId, Long itemSustitutoId, BigDecimal cantidad,
			BigDecimal diferenciaPrecio, LocalDateTime fecha, String observaciones) {
		this.pedidoId = pedidoId;
		this.itemOriginalId = itemOriginalId;
		this.itemSustitutoId = itemSustitutoId;
		this.cantidad = cantidad;
		this.diferenciaPrecio = diferenciaPrecio;
		this.fecha = fecha;
		this.observaciones = observaciones;
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
