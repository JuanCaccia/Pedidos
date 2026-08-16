package com.sistema.categoria.port.in;

import com.sistema.categoria.model.Categoria;

public interface GestionarCategoria {

	record CrearCategoriaCommand(String nombre) {
	}

	record ActualizarCategoriaCommand(Long categoriaId, String nombre) {
	}

	Categoria crear(CrearCategoriaCommand command);

	Categoria actualizar(ActualizarCategoriaCommand command);

	void desactivar(Long categoriaId);

	void reactivar(Long categoriaId);
}
