package com.sistema.cliente.service;

import com.sistema.cliente.model.Cliente;
import com.sistema.cliente.port.in.ConsultarCliente;
import com.sistema.cliente.port.in.GestionarCliente;
import com.sistema.cliente.port.out.ClienteRepository;
import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.Zona;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClienteService implements GestionarCliente, ConsultarCliente {

	private final ClienteRepository clienteRepository;
	private final ZonaRepository zonaRepository;

	public ClienteService(ClienteRepository clienteRepository, ZonaRepository zonaRepository) {
		this.clienteRepository = clienteRepository;
		this.zonaRepository = zonaRepository;
	}

	@Override
	@Transactional
	public Cliente crearCliente(CrearClienteCommand command) {
		validarDatosBasicos(command.razonSocial(), command.cuit());
		String cuit = command.cuit().trim();
		clienteRepository.findByCuit(cuit).ifPresent(c -> {
			throw new BusinessException("CLIENTE_CUIT_DUPLICADO", "A client with that CUIT already exists");
		});
		Zona zona = obtenerZonaO404(command.zonaId());
		Cliente cliente = new Cliente(command.razonSocial().trim(), cuit, zona);
		cliente.setEmail(command.email());
		cliente.setTelefono(command.telefono());
		cliente.setDomicilio(command.domicilio());
		return clienteRepository.save(cliente);
	}

	@Override
	@Transactional
	public Cliente actualizarCliente(ActualizarClienteCommand command) {
		Cliente cliente = obtenerClienteO404(command.clienteId());
		Zona zona = obtenerZonaO404(command.zonaId());
		cliente.actualizar(command.razonSocial().trim(), command.email(), command.telefono(), command.domicilio(), zona);
		return clienteRepository.save(cliente);
	}

	@Override
	@Transactional
	public void desactivarCliente(Long clienteId) {
		Cliente cliente = obtenerClienteO404(clienteId);
		cliente.desactivar();
		clienteRepository.save(cliente);
	}

	@Override
	@Transactional
	public void reactivarCliente(Long clienteId) {
		Cliente cliente = obtenerClienteO404(clienteId);
		cliente.reactivar();
		clienteRepository.save(cliente);
	}

	@Override
	public Optional<Cliente> buscarPorId(Long id) {
		return clienteRepository.findById(id);
	}

	@Override
	public Optional<Cliente> buscarPorCuit(String cuit) {
		return clienteRepository.findByCuit(cuit.trim());
	}

	@Override
	public List<Cliente> listarTodos() {
		return clienteRepository.findAll();
	}

	@Override
	public List<Cliente> listarPorZona(Long zonaId) {
		return clienteRepository.findByZonaId(zonaId);
	}

	private Cliente obtenerClienteO404(Long clienteId) {
		return clienteRepository.findById(clienteId)
				.orElseThrow(() -> new NotFoundException("Client not found: " + clienteId));
	}

	private Zona obtenerZonaO404(Long zonaId) {
		return zonaRepository.findById(zonaId)
				.orElseThrow(() -> new NotFoundException("Zone not found: " + zonaId));
	}

	private void validarDatosBasicos(String razonSocial, String cuit) {
		if (razonSocial == null || razonSocial.isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Business name is required");
		}
		if (cuit == null || !cuit.trim().matches("^\\d{11}$")) {
			throw new BusinessException("VALIDATION_ERROR", "CUIT must be exactly 11 digits");
		}
	}
}
