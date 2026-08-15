package com.sistema.cliente.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {

	Optional<ClienteJpaEntity> findByCuit(String cuit);

	List<ClienteJpaEntity> findByZonaId(Long zonaId);

	@Query("select c from ClienteJpaEntity c where (cast(:q as string) is null or lower(c.razonSocial) like lower(concat('%', cast(:q as string), '%')) or lower(c.cuit) like lower(concat('%', cast(:q as string), '%'))) and (:zonaId is null or c.zona.id = :zonaId)")
	Page<ClienteJpaEntity> buscar(@Param("q") String q, @Param("zonaId") Long zonaId, Pageable pageable);
}
