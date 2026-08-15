package com.sistema.compra.adapter.out.persistence;

import com.sistema.compra.model.EstadoOrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenCompraJpaRepository extends JpaRepository<OrdenCompraJpaEntity, Long> {

	List<OrdenCompraJpaEntity> findByEstado(EstadoOrdenCompra estado);

	List<OrdenCompraJpaEntity> findByProveedorId(Long proveedorId);
}
