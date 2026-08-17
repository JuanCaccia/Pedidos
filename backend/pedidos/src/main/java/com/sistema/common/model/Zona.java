package com.sistema.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "zona")
public class Zona extends BaseEntity {

	@Column(nullable = false, unique = true, length = 100)
	private String nombre;

	@Column(nullable = false)
	private boolean activo = true;

	protected Zona() {
		// required by JPA
	}

	public Zona(String nombre) {
		this.nombre = nombre;
	}

	public void renombrar(String nombre) {
		this.nombre = nombre;
	}

	public void desactivar() {
		this.activo = false;
	}

	public void reactivar() {
		this.activo = true;
	}

	public String getNombre() {
		return nombre;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
