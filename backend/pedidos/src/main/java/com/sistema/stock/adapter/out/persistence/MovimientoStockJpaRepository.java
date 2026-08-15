package com.sistema.stock.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockJpaRepository extends JpaRepository<MovimientoStockJpaEntity, Long> {

	List<MovimientoStockJpaEntity> findByItemIdOrderByFechaAsc(Long itemId);

	Page<MovimientoStockJpaEntity> findByItemIdOrderByFechaAsc(Long itemId, Pageable pageable);

	List<MovimientoStockJpaEntity> findByLoteId(Long loteId);

	List<MovimientoStockJpaEntity> findByPedidoId(Long pedidoId);
}
