package com.sistema.cliente.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {

	Optional<ClienteJpaEntity> findByCuit(String cuit);

	List<ClienteJpaEntity> findByZonaId(Long zonaId);
}
