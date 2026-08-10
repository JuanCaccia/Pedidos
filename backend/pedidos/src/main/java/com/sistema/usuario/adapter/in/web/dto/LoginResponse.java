package com.sistema.usuario.adapter.in.web.dto;

import com.sistema.usuario.model.Usuario;

import java.util.List;

public record LoginResponse(String token, Long usuarioId, String email, List<String> roles) {

	public static LoginResponse of(String token, Usuario usuario) {
		return new LoginResponse(token, usuario.getId(), usuario.getEmail(),
				usuario.getRoles().stream().map(Enum::name).toList());
	}
}
