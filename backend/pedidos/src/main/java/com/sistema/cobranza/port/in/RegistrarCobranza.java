package com.sistema.cobranza.port.in;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.model.FormaPago;
import com.sistema.usuario.model.Usuario;

import java.math.BigDecimal;

public interface RegistrarCobranza {

	record RegistrarCobranzaCommand(Long clienteId, Long pedidoId, BigDecimal monto, FormaPago formaPago, String observaciones,
			Usuario actor) {
	}

	Cobranza registrar(RegistrarCobranzaCommand command);
}
