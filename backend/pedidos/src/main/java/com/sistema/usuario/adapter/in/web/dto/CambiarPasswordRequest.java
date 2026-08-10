package com.sistema.usuario.adapter.in.web.dto;

import jakarta.validation.constraints.*;

public record CambiarPasswordRequest(@NotBlank @Size(min = 6) String password) {
}
