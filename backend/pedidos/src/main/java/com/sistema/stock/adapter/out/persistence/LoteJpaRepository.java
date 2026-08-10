package com.sistema.stock.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LoteJpaRepository extends JpaRepository<LoteJpaEntity, Long> {

	List<LoteJpaEntity> findByItemId(Long itemId);

	List<LoteJpaEntity> findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(LocalDate fecha);
}
