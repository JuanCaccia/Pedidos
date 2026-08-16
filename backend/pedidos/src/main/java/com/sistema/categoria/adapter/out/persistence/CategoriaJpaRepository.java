package com.sistema.categoria.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaEntity, Long> {

	Optional<CategoriaJpaEntity> findByNombre(String nombre);
}
