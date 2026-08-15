package com.sistema.cobranza.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CobranzaJpaRepository extends JpaRepository<CobranzaJpaEntity, Long> {

	List<CobranzaJpaEntity> findByClienteId(Long clienteId);
}
