package com.sistema.usuario.adapter.out.persistence;

import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.out.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

	private final UsuarioJpaRepository jpaRepository;
	private final UsuarioMapper mapper = new UsuarioMapper();

	public UsuarioRepositoryAdapter(UsuarioJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Usuario save(Usuario usuario) {
		UsuarioJpaEntity entity = mapper.toJpa(usuario);
		UsuarioJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Usuario> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Usuario> findByEmail(String email) {
		return jpaRepository.findByEmail(email).map(mapper::toDomain);
	}

	@Override
	public List<Usuario> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public PageResponse<Usuario> buscar(String q, int page, int size) {
		return PageMapper.of(jpaRepository.buscar(normalizar(q), PageRequest.of(page, size)), mapper::toDomain);
	}

	private String normalizar(String q) {
		return q == null || q.isBlank() ? null : q.trim();
	}
}
