package com.sistema.stock.model;

import java.math.BigDecimal;

public class Item {

	private Long id;
	private String sku;
	private String nombre;
	private String unidadMedida;
	private boolean activo = true;
	private BigDecimal stockMinimo = BigDecimal.ZERO;
	private BigDecimal precioLista = BigDecimal.ZERO;
	private String categoria;

	public Item() {
	}

	public Item(String sku, String nombre, String unidadMedida) {
		this.sku = sku;
		this.nombre = nombre;
		this.unidadMedida = unidadMedida;
		this.stockMinimo = BigDecimal.ZERO;
		this.precioLista = BigDecimal.ZERO;
	}

	public void desactivar() {
		this.activo = false;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getUnidadMedida() {
		return unidadMedida;
	}

	public void setUnidadMedida(String unidadMedida) {
		this.unidadMedida = unidadMedida;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public BigDecimal getStockMinimo() {
		return stockMinimo;
	}

	public void setStockMinimo(BigDecimal stockMinimo) {
		this.stockMinimo = stockMinimo;
	}

	public BigDecimal getPrecioLista() {
		return precioLista;
	}

	public void setPrecioLista(BigDecimal precioLista) {
		this.precioLista = precioLista;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
}
