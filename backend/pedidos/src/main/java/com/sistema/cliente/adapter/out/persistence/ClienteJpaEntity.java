package com.sistema.cliente.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.common.model.Zona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class ClienteJpaEntity extends BaseEntity {

	@Column(name = "razon_social", nullable = false, length = 200)
	private String razonSocial;

	@Column(nullable = false, length = 20, unique = true)
	private String cuit;

	@Column(length = 150)
	private String email;

	@Column(length = 50)
	private String telefono;

	@Column(length = 255)
	private String domicilio;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "zona_id", nullable = false)
	private Zona zona;

	@Column(nullable = false)
	private boolean activo = true;

	protected ClienteJpaEntity() {
		// required by JPA
	}

	public ClienteJpaEntity(String razonSocial, String cuit, Zona zona) {
		this.razonSocial = razonSocial;
		this.cuit = cuit;
		this.zona = zona;
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
