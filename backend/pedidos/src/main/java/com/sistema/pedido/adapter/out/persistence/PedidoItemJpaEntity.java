package com.sistema.pedido.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_item")
public class PedidoItemJpaEntity extends BaseEntity {

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "cantidad_pedida", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadPedida;

	@Column(name = "cantidad_reservada", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadReservada;

	@Column(name = "cantidad_entregada", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadEntregada;

	@Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(name = "pendiente_stock", nullable = false)
	private boolean pendienteStock;

	protected PedidoItemJpaEntity() {
		// required by JPA
	}

	public PedidoItemJpaEntity(Long itemId, BigDecimal cantidadPedida, BigDecimal cantidadReservada,
			BigDecimal cantidadEntregada, BigDecimal precioUnitario, boolean pendienteStock) {
		this.itemId = itemId;
		this.cantidadPedida = cantidadPedida;
		this.cantidadReservada = cantidadReservada;
		this.cantidadEntregada = cantidadEntregada;
		this.precioUnitario = precioUnitario;
		this.pendienteStock = pendienteStock;
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
