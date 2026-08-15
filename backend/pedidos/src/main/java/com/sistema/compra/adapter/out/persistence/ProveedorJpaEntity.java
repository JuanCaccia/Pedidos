package com.sistema.compra.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedor")
public class ProveedorJpaEntity extends BaseEntity {

	@Column(name = "razon_social", nullable = false, length = 200)
	private String razonSocial;

	@Column(nullable = false, length = 20, unique = true)
	private String cuit;

	@Column(length = 150)
	private String email;

	@Column(length = 50)
	private String telefono;

	@Column(nullable = false)
	private boolean activo = true;

	protected ProveedorJpaEntity() {
		// required by JPA
	}

	public ProveedorJpaEntity(String razonSocial, String cuit) {
		this.razonSocial = razonSocial;
		this.cuit = cuit;
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
