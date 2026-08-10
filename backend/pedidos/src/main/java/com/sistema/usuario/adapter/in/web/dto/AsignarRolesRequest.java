package com.sistema.usuario.adapter.in.web.dto;

import com.sistema.usuario.model.Rol;
import jakarta.validation.constraints.*;

import java.util.Set;

public record AsignarRolesRequest(@NotEmpty Set<Rol> roles) {
}
