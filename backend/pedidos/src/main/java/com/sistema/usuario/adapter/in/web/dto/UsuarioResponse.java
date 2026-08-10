package com.sistema.usuario.adapter.in.web.dto;

import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;

import java.util.Set;

public record UsuarioResponse(Long id, String nombre, String email, boolean activo, Set<Rol> roles) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
				usuario.isActivo(), usuario.getRoles());
	}
}
