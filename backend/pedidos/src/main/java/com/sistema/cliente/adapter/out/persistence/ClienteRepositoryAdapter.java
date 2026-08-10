package com.sistema.cliente.adapter.out.persistence;

import com.sistema.cliente.model.Cliente;
import com.sistema.cliente.port.out.ClienteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepositoryAdapter implements ClienteRepository {

	private final ClienteJpaRepository jpaRepository;
	private final ClienteMapper mapper = new ClienteMapper();

	public ClienteRepositoryAdapter(ClienteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Cliente save(Cliente cliente) {
		ClienteJpaEntity entity = mapper.toJpa(cliente);
		ClienteJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Cliente> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Cliente> findByCuit(String cuit) {
		return jpaRepository.findByCuit(cuit).map(mapper::toDomain);
	}

	@Override
	public List<Cliente> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Cliente> findByZonaId(Long zonaId) {
		return jpaRepository.findByZonaId(zonaId).stream().map(mapper::toDomain).toList();
	}
}
