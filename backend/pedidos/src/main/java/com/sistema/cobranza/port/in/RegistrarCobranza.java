package com.sistema.cobranza.port.in;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.model.FormaPago;

import java.math.BigDecimal;

public interface RegistrarCobranza {

	record RegistrarCobranzaCommand(Long clienteId, Long pedidoId, BigDecimal monto, FormaPago formaPago, String observaciones) {
	}

	Cobranza registrar(RegistrarCobranzaCommand command);
}
