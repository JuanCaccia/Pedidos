package com.sistema.stock.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Query("select i from ItemJpaEntity i where (:q is null or lower(cast(:q as string)) = '' or lower(i.sku) like lower(concat('%', cast(:q as string), '%')) or lower(i.nombre) like lower(concat('%', cast(:q as string), '%'))) and (:categoriaId is null or i.categoriaId = :categoriaId)")
	Page<ItemJpaEntity> buscar(@Param("q") String q, @Param("categoriaId") Long categoriaId, Pageable pageable);

	@Query("select i from ItemJpaEntity i where i.activo = true and (:q is null or lower(cast(:q as string)) = '' or lower(i.sku) like lower(concat('%', cast(:q as string), '%')) or lower(i.nombre) like lower(concat('%', cast(:q as string), '%'))) and (:categoriaId is null or i.categoriaId = :categoriaId)")
	Page<ItemJpaEntity> buscarActivos(@Param("q") String q, @Param("categoriaId") Long categoriaId, Pageable pageable);
}
