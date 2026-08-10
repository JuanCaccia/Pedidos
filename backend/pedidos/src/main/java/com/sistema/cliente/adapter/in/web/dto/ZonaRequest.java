package com.sistema.cliente.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record ZonaRequest(@NotBlank String nombre) {
}
