package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProveedorRequest(@NotBlank String razonSocial, @NotBlank @Pattern(regexp = "^\\d{11}$") String cuit,
		String email, String telefono) {
}
