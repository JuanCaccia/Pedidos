package com.sistema.ruta.adapter.out.persistence;

import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.out.RutaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class RutaRepositoryAdapter implements RutaRepository {

	private final RutaJpaRepository jpaRepository;
	private final RutaMapper mapper = new RutaMapper();

	public RutaRepositoryAdapter(RutaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Ruta save(Ruta ruta) {
		RutaJpaEntity entity = mapper.toJpa(ruta);
		RutaJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Ruta> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Ruta> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Ruta> findByFechaJornada(LocalDate fechaJornada) {
		return jpaRepository.findByFechaJornada(fechaJornada).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Ruta> findByRepartidorId(Long repartidorId) {
		return jpaRepository.findByRepartidorId(repartidorId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Ruta> findByEstado(EstadoRuta estado) {
		return jpaRepository.findByEstado(estado).stream().map(mapper::toDomain).toList();
	}
}
