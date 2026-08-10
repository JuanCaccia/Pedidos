package com.sistema.cliente.port.out;

import com.sistema.cliente.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

	Cliente save(Cliente cliente);

	Optional<Cliente> findById(Long id);

	Optional<Cliente> findByCuit(String cuit);

	List<Cliente> findAll();

	List<Cliente> findByZonaId(Long zonaId);
}
