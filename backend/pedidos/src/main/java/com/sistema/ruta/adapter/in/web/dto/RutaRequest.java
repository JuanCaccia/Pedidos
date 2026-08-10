package com.sistema.ruta.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record RutaRequest(@NotNull Long zonaId, @NotNull Long repartidorId, @NotNull LocalDate fechaJornada,
		@NotEmpty List<Long> pedidoIds) {
}
