package com.sistema.usuario.adapter.out.persistence;

import com.sistema.usuario.model.Usuario;

public class UsuarioMapper {

	public Usuario toDomain(UsuarioJpaEntity entity) {
		Usuario usuario = new Usuario(entity.getNombre(), entity.getEmail(), entity.getPasswordHash(), entity.getRoles());
		usuario.setId(entity.getId());
		usuario.setActivo(entity.isActivo());
		return usuario;
	}

	public UsuarioJpaEntity toJpa(Usuario usuario) {
		UsuarioJpaEntity entity = new UsuarioJpaEntity(usuario.getNombre(), usuario.getEmail(), usuario.getPasswordHash(),
				usuario.isActivo(), usuario.getRoles());
		if (usuario.getId() != null) {
			entity.setId(usuario.getId());
		}
		return entity;
	}
}
