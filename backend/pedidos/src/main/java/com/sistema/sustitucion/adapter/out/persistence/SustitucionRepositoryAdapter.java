package com.sistema.sustitucion.adapter.out.persistence;

import com.sistema.sustitucion.model.Sustitucion;
import com.sistema.sustitucion.port.out.SustitucionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SustitucionRepositoryAdapter implements SustitucionRepository {

	private final SustitucionJpaRepository jpaRepository;
	private final SustitucionMapper mapper = new SustitucionMapper();

	public SustitucionRepositoryAdapter(SustitucionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Sustitucion save(Sustitucion s) {
		return mapper.toDomain(jpaRepository.save(mapper.toJpa(s)));
	}
}
