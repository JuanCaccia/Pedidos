package com.sistema.pedido.port.out;

import java.util.Optional;

public interface ClienteGateway {

	boolean existeCliente(Long clienteId);

	boolean clienteActivo(Long clienteId);

	Optional<Long> zonaDeCliente(Long clienteId);
}
