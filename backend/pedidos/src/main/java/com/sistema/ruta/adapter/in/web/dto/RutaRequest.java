package com.sistema.ruta.adapter.in.web.dto;

import java.time.LocalDate;
import java.util.List;

public record RutaRequest(Long zonaId, Long repartidorId, LocalDate fechaJornada, List<Long> pedidoIds) {
}
