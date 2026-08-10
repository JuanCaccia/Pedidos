package com.sistema.cliente.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record ActualizarClienteRequest(@NotBlank String razonSocial, String email, String telefono, String domicilio,
		@NotNull Long zonaId) {
}
