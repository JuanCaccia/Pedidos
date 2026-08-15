package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.cobranza.model.Remito;
import com.sistema.cobranza.port.out.RemitoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RemitoRepositoryAdapter implements RemitoRepository {

	private final RemitoJpaRepository jpaRepository;
	private final RemitoMapper mapper = new RemitoMapper();

	public RemitoRepositoryAdapter(RemitoJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Remito save(Remito remito) {
		RemitoJpaEntity entity = mapper.toJpa(remito);
		RemitoJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Remito> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Remito> findByPedidoId(Long pedidoId) {
		return jpaRepository.findByPedidoId(pedidoId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Remito> findByClienteId(Long clienteId) {
		return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Remito> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}
}
