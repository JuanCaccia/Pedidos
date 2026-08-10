package com.sistema.cliente.model;

import com.sistema.common.model.Zona;

public class Cliente {

	private Long id;
	private String razonSocial;
	private String cuit;
	private String email;
	private String telefono;
	private String domicilio;
	private Zona zona;
	private boolean activo = true;

	public Cliente() {
	}

	public Cliente(String razonSocial, String cuit, Zona zona) {
		this.razonSocial = razonSocial;
		this.cuit = cuit;
		this.zona = zona;
	}

	public void actualizar(String razonSocial, String email, String telefono, String domicilio, Zona zona) {
		this.razonSocial = razonSocial;
		this.email = email;
		this.telefono = telefono;
		this.domicilio = domicilio;
		this.zona = zona;
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

	public String getRazonSocial() {
		return razonSocial;
	}

	public void setRazonSocial(String razonSocial) {
		this.razonSocial = razonSocial;
	}

	public String getCuit() {
		return cuit;
	}

	public void setCuit(String cuit) {
		this.cuit = cuit;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getDomicilio() {
		return domicilio;
	}

	public void setDomicilio(String domicilio) {
		this.domicilio = domicilio;
	}

	public Zona getZona() {
		return zona;
	}

	public void setZona(Zona zona) {
		this.zona = zona;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
