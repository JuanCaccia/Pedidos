package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarProveedorRequest(@NotBlank String razonSocial, String email, String telefono) {
}
