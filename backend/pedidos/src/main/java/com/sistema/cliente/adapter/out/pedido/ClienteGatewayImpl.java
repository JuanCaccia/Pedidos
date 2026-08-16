package com.sistema.cliente.adapter.out.pedido;

import com.sistema.cliente.port.out.ClienteRepository;
import com.sistema.pedido.port.out.ClienteGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ClienteGatewayImpl implements ClienteGateway {

	private final ClienteRepository clienteRepository;

	public ClienteGatewayImpl(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	@Override
	public boolean existeCliente(Long clienteId) {
		return clienteRepository.findById(clienteId).isPresent();
	}

	@Override
	public boolean clienteActivo(Long clienteId) {
		return clienteRepository.findById(clienteId)
				.map(com.sistema.cliente.model.Cliente::isActivo)
				.orElse(false);
	}

	@Override
	public Optional<Long> zonaDeCliente(Long clienteId) {
		return clienteRepository.findById(clienteId)
				.map(c -> c.getZona() != null ? c.getZona().getId() : null);
	}
}
