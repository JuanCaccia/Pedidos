package com.sistema.stock.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
}
