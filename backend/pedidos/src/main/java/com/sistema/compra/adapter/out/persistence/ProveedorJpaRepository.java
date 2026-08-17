package com.sistema.compra.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorJpaEntity, Long> {

	Optional<ProveedorJpaEntity> findByCuit(String cuit);

	@Query("select p from ProveedorJpaEntity p where cast(:q as string) is null or lower(p.razonSocial) like lower(concat('%', cast(:q as string), '%')) or lower(p.cuit) like lower(concat('%', cast(:q as string), '%'))")
	Page<ProveedorJpaEntity> buscar(@Param("q") String q, Pageable pageable);

	@Query("select p from ProveedorJpaEntity p where p.id in "
			+ "(select pi.proveedorId from ProveedorItemJpaEntity pi where pi.itemId = :itemId and pi.activo = true)")
	List<ProveedorJpaEntity> findProveedoresDeItemActivo(@Param("itemId") Long itemId);
}
