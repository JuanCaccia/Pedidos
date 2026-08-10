package com.sistema.stock.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemJpaRepository extends JpaRepository<ItemJpaEntity, Long> {

	Optional<ItemJpaEntity> findBySku(String sku);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from ItemJpaEntity i where i.id = :id")
	Optional<ItemJpaEntity> findByIdParaActualizar(@Param("id") Long id);
}
