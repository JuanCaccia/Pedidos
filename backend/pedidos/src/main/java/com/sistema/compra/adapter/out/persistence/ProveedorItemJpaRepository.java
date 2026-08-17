package com.sistema.compra.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProveedorItemJpaRepository extends JpaRepository<ProveedorItemJpaEntity, ProveedorItemId> {

	void deleteByProveedorId(Long proveedorId);

	List<ProveedorItemJpaEntity> findByProveedorId(Long proveedorId);

	List<ProveedorItemJpaEntity> findByProveedorIdAndActivoTrue(Long proveedorId);

	boolean existsByProveedorIdAndItemIdAndActivoTrue(Long proveedorId, Long itemId);

	@Query(value = "SELECT pi.proveedor_id, pi.item_id, i.sku, i.nombre, pi.activo "
			+ "FROM proveedor_item pi JOIN item i ON i.id = pi.item_id "
			+ "WHERE pi.proveedor_id = :proveedorId "
			+ "AND (:soloActivos = false OR pi.activo = TRUE) "
			+ "ORDER BY i.nombre", nativeQuery = true)
	List<Object[]> listarItems(@Param("proveedorId") Long proveedorId, @Param("soloActivos") boolean soloActivos);
}
