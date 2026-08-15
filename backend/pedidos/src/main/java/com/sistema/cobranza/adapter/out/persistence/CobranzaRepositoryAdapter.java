package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.port.out.CobranzaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CobranzaRepositoryAdapter implements CobranzaRepository {

	private final CobranzaJpaRepository jpaRepository;
	private final CobranzaMapper mapper = new CobranzaMapper();

	public CobranzaRepositoryAdapter(CobranzaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Cobranza save(Cobranza cobranza) {
		CobranzaJpaEntity entity = mapper.toJpa(cobranza);
		CobranzaJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Cobranza> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Cobranza> findByClienteId(Long clienteId) {
		return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Cobranza> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}
}
