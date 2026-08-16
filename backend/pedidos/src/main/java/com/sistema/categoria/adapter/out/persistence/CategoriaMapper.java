package com.sistema.categoria.adapter.out.persistence;

import com.sistema.categoria.model.Categoria;

public class CategoriaMapper {

	public Categoria toDomain(CategoriaJpaEntity entity) {
		Categoria categoria = new Categoria(entity.getNombre());
		categoria.setId(entity.getId());
		categoria.setActivo(entity.isActivo());
		return categoria;
	}

	public CategoriaJpaEntity toJpa(Categoria categoria) {
		CategoriaJpaEntity entity = new CategoriaJpaEntity(categoria.getNombre());
		if (categoria.getId() != null) {
			entity.setId(categoria.getId());
		}
		entity.setActivo(categoria.isActivo());
		return entity;
	}
}
