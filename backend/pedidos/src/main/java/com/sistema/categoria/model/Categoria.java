package com.sistema.categoria.model;

public class Categoria {

	private Long id;
	private String nombre;
	private boolean activo = true;

	public Categoria() {
	}

	public Categoria(String nombre) {
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
