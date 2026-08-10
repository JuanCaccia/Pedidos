package com.sistema.usuario.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Usuario {

	private Long id;
	private String nombre;
	private String email;
	private String passwordHash;
	private boolean activo = true;
	private Set<Rol> roles = new LinkedHashSet<>();

	public Usuario() {
	}

	public Usuario(String nombre, String email, String passwordHash, Set<Rol> roles) {
		this.nombre = nombre;
		this.email = email;
		this.passwordHash = passwordHash;
		this.roles = new LinkedHashSet<>(roles);
	}

	public void asignarRoles(Set<Rol> nuevosRoles) {
		this.roles.addAll(nuevosRoles);
	}

	public boolean tieneRol(Rol rol) {
		return roles.contains(rol);
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public Set<Rol> getRoles() {
		return Collections.unmodifiableSet(roles);
	}
}
