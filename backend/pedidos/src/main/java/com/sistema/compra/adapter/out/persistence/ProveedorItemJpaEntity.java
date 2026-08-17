package com.sistema.compra.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedor_item")
@IdClass(ProveedorItemId.class)
public class ProveedorItemJpaEntity {

	@Id
	@Column(name = "proveedor_id", nullable = false)
	private Long proveedorId;

	@Id
	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(nullable = false)
	private boolean activo = true;

	protected ProveedorItemJpaEntity() {
		// required by JPA
	}

	public ProveedorItemJpaEntity(Long proveedorId, Long itemId) {
		this.proveedorId = proveedorId;
		this.itemId = itemId;
	}

	public Long getProveedorId() {
		return proveedorId;
	}

	public void setProveedorId(Long proveedorId) {
		this.proveedorId = proveedorId;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
