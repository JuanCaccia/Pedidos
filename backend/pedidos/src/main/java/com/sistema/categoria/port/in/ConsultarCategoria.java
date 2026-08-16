package com.sistema.categoria.port.in;

import com.sistema.categoria.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface ConsultarCategoria {

	List<Categoria> listarActivas();

	List<Categoria> listarTodas();

	Optional<Categoria> buscarPorId(Long id);
}
