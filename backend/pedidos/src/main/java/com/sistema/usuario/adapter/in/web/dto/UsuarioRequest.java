package com.sistema.usuario.adapter.in.web.dto;

import com.sistema.usuario.model.Rol;
import jakarta.validation.constraints.*;

import java.util.Set;

public record UsuarioRequest(@NotBlank String nombre, @NotBlank @Email String email,
		@NotBlank @Size(min = 6) String password, @NotEmpty Set<Rol> roles) {
}
