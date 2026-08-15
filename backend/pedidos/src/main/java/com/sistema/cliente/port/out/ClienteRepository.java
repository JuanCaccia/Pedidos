package com.sistema.cliente.port.out;

import com.sistema.cliente.model.Cliente;
import com.sistema.common.model.PageResponse;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

	Cliente save(Cliente cliente);

	Optional<Cliente> findById(Long id);

	Optional<Cliente> findByCuit(String cuit);

	List<Cliente> findAll();

	List<Cliente> findByZonaId(Long zonaId);

	PageResponse<Cliente> buscar(String q, Long zonaId, int page, int size);
}
