package com.sistema.categoria.adapter.out.persistence;

import com.sistema.categoria.model.Categoria;
import com.sistema.categoria.port.out.CategoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaRepositoryAdapter implements CategoriaRepository {

	private final CategoriaJpaRepository jpaRepository;
	private final CategoriaMapper mapper = new CategoriaMapper();

	public CategoriaRepositoryAdapter(CategoriaJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Categoria save(Categoria categoria) {
		CategoriaJpaEntity entity = mapper.toJpa(categoria);
		CategoriaJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Categoria> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Categoria> findByNombre(String nombre) {
		return jpaRepository.findByNombre(nombre).map(mapper::toDomain);
	}

	@Override
	public List<Categoria> findAll() {
		return jpaRepository.findAll(Sort.by("nombre")).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Categoria> findByActivoTrue() {
		return jpaRepository.findAll(Sort.by("nombre")).stream()
				.filter(CategoriaJpaEntity::isActivo)
				.map(mapper::toDomain)
				.toList();
	}
}
