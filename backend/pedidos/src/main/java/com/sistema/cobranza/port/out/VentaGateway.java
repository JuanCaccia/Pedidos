package com.sistema.cobranza.port.out;

import java.math.BigDecimal;

public interface VentaGateway {

	BigDecimal totalVendidoCliente(Long clienteId);
}
