package com.sistema.compra.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;

public class ProveedorItemId implements Serializable {

	private Long proveedorId;
	private Long itemId;

	protected ProveedorItemId() {
		// required by JPA
	}

	public ProveedorItemId(Long proveedorId, Long itemId) {
		this.proveedorId = proveedorId;
		this.itemId = itemId;
	}

	public Long getProveedorId() {
		return proveedorId;
	}

	public Long getItemId() {
		return itemId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProveedorItemId that)) {
			return false;
		}
		return Objects.equals(proveedorId, that.proveedorId) && Objects.equals(itemId, that.itemId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(proveedorId, itemId);
	}
}
