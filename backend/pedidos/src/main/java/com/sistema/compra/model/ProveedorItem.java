package com.sistema.compra.model;

/**
 * Relación proveedor ↔ item (catálogo de provisión).
 * Agrega datos de lectura del item (sku, nombre) para listar sin acoplar a stock.
 */
public class ProveedorItem {

	private Long proveedorId;
	private Long itemId;
	private String itemSku;
	private String itemNombre;
	private boolean activo = true;

	public ProveedorItem() {
	}

	public ProveedorItem(Long proveedorId, Long itemId) {
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

	public String getItemSku() {
		return itemSku;
	}

	public void setItemSku(String itemSku) {
		this.itemSku = itemSku;
	}

	public String getItemNombre() {
		return itemNombre;
	}

	public void setItemNombre(String itemNombre) {
		this.itemNombre = itemNombre;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
