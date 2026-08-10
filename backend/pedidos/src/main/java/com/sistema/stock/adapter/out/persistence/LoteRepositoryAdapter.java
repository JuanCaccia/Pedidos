package com.sistema.stock.adapter.out.persistence;

import com.sistema.stock.model.Lote;
import com.sistema.stock.port.out.LoteRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class LoteRepositoryAdapter implements LoteRepository {

	private final LoteJpaRepository jpaRepository;
	private final LoteMapper mapper = new LoteMapper();

	public LoteRepositoryAdapter(LoteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Lote save(Lote lote) {
		LoteJpaEntity entity = mapper.toJpa(lote);
		LoteJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Lote> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Lote> findByItemId(Long itemId) {
		return jpaRepository.findByItemId(itemId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Lote> findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(LocalDate fecha) {
		return jpaRepository.findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(fecha).stream()
				.map(mapper::toDomain)
				.toList();
	}
}
