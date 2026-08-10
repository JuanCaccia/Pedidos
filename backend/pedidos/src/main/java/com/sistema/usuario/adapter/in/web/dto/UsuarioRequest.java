package com.sistema.usuario.adapter.in.web.dto;

import com.sistema.usuario.model.Rol;

import java.util.Set;

public record UsuarioRequest(String nombre, String email, String password, Set<Rol> roles) {
}
