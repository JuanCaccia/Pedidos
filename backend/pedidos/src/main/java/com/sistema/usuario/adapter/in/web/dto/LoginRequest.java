package com.sistema.usuario.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
