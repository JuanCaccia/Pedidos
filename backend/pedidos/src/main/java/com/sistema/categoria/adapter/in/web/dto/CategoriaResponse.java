package com.sistema.categoria.adapter.in.web.dto;

import com.sistema.categoria.model.Categoria;

public record CategoriaResponse(Long id, String nombre, boolean activo) {

	public static CategoriaResponse from(Categoria categoria) {
		return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.isActivo());
	}
}
