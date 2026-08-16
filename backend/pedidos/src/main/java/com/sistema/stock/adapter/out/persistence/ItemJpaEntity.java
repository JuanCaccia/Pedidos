package com.sistema.stock.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "item")
public class ItemJpaEntity extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String sku;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(name = "unidad_medida", nullable = false, length = 20)
	private String unidadMedida;

	@Column(nullable = false)
	private boolean activo = true;

	@Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
	private BigDecimal stockMinimo = BigDecimal.ZERO;

	@Column(name = "precio_lista", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioLista = BigDecimal.ZERO;

	@Column(name = "categoria_id")
	private Long categoriaId;

	protected ItemJpaEntity() {
		// required by JPA
	}

	public ItemJpaEntity(String sku, String nombre, String unidadMedida) {
		this.sku = sku;
		this.nombre = nombre;
		this.unidadMedida = unidadMedida;
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

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long categoriaId) {
		this.categoriaId = categoriaId;
	}
}
