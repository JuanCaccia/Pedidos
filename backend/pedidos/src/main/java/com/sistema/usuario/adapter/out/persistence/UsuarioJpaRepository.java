package com.sistema.usuario.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

	Optional<UsuarioJpaEntity> findByEmail(String email);

	@Query("select u from UsuarioJpaEntity u where cast(:q as string) is null or lower(u.nombre) like lower(concat('%', cast(:q as string), '%')) or lower(u.email) like lower(concat('%', cast(:q as string), '%'))")
	Page<UsuarioJpaEntity> buscar(@Param("q") String q, Pageable pageable);
}
