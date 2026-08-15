package com.sistema.compra.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_compra_linea")
public class OrdenCompraLineaJpaEntity extends BaseEntity {

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "cantidad_pedida", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadPedida;

	@Column(name = "cantidad_recibida", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadRecibida;

	@Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	protected OrdenCompraLineaJpaEntity() {
		// required by JPA
	}

	public OrdenCompraLineaJpaEntity(Long itemId, BigDecimal cantidadPedida, BigDecimal cantidadRecibida,
			BigDecimal precioUnitario) {
		this.itemId = itemId;
		this.cantidadPedida = cantidadPedida;
		this.cantidadRecibida = cantidadRecibida;
		this.precioUnitario = precioUnitario;
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
