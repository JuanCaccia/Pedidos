package com.sistema.cliente.adapter.out.persistence;

import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.model.Zona;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ZonaRepositoryAdapter implements ZonaRepository {

	private final ZonaJpaRepository jpaRepository;

	public ZonaRepositoryAdapter(ZonaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Zona save(Zona zona) {
		return jpaRepository.save(zona);
	}

	@Override
	public Optional<Zona> findById(Long id) {
		return jpaRepository.findById(id);
	}

	@Override
	public Optional<Zona> findByNombre(String nombre) {
		return jpaRepository.findByNombre(nombre);
	}

	@Override
	public List<Zona> findAll() {
		return jpaRepository.findAll();
	}
}
