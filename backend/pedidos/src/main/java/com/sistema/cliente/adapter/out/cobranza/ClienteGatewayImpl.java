package com.sistema.cliente.adapter.out.cobranza;

import com.sistema.cliente.port.out.ClienteRepository;
import com.sistema.cobranza.port.out.ClienteGateway;
import org.springframework.stereotype.Component;

@Component("clienteCobranzaGateway")
public class ClienteGatewayImpl implements ClienteGateway {

	private final ClienteRepository clienteRepository;

	public ClienteGatewayImpl(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	@Override
	public boolean existeCliente(Long clienteId) {
		return clienteRepository.findById(clienteId).isPresent();
	}
}
