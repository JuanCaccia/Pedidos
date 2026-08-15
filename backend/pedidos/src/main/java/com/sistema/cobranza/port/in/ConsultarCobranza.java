package com.sistema.cobranza.port.in;

import com.sistema.cobranza.model.Cobranza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConsultarCobranza {

	record EstadoCuenta(Long clienteId, BigDecimal totalVendido, BigDecimal totalCobrado, BigDecimal saldo) {
	}

	List<Cobranza> listar(Long clienteId, LocalDate desde, LocalDate hasta);

	EstadoCuenta estadoCuenta(Long clienteId);
}
