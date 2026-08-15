package com.sistema.compra.model;

public class Proveedor {

	private Long id;
	private String razonSocial;
	private String cuit;
	private String email;
	private String telefono;
	private boolean activo = true;

	public Proveedor() {
	}

	public Proveedor(String razonSocial, String cuit) {
		this.razonSocial = razonSocial;
		this.cuit = cuit;
	}

	public void actualizar(String razonSocial, String email, String telefono) {
		this.razonSocial = razonSocial;
		this.email = email;
		this.telefono = telefono;
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

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
