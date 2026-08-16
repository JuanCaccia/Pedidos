package com.sistema.categoria.port.out;

import com.sistema.categoria.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

	Categoria save(Categoria categoria);

	Optional<Categoria> findById(Long id);

	Optional<Categoria> findByNombre(String nombre);

	List<Categoria> findAll();

	List<Categoria> findByActivoTrue();
}
