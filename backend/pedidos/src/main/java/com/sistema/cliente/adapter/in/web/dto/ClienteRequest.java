package com.sistema.cliente.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record ClienteRequest(@NotBlank String razonSocial, @NotBlank String cuit, String email, String telefono,
		String domicilio, @NotNull Long zonaId) {
}
