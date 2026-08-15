package com.sistema.cobranza.adapter.in.web.dto;

import com.sistema.cobranza.model.FormaPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CobranzaRequest(Long clienteId, Long pedidoId,
		@NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero") BigDecimal monto,
		@NotNull FormaPago formaPago, String observaciones) {
}
