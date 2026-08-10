package com.sistema.stock.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.stock.model.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_stock")
public class MovimientoStockJpaEntity extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TipoMovimiento tipo;

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "lote_id")
	private Long loteId;

	@Column(name = "pedido_id")
	private Long pedidoId;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidad;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Column(length = 255)
	private String motivo;

	protected MovimientoStockJpaEntity() {
		// required by JPA
	}

	public MovimientoStockJpaEntity(TipoMovimiento tipo, Long itemId, Long loteId, Long pedidoId,
			BigDecimal cantidad, LocalDateTime fecha, String motivo) {
		this.tipo = tipo;
		this.itemId = itemId;
		this.loteId = loteId;
		this.pedidoId = pedidoId;
		this.cantidad = cantidad;
		this.fecha = fecha;
		this.motivo = motivo;
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
