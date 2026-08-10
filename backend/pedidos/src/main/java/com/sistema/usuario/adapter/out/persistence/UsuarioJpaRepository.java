package com.sistema.usuario.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

	Optional<UsuarioJpaEntity> findByEmail(String email);
}
