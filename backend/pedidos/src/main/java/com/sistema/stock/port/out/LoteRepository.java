package com.sistema.stock.port.out;

import com.sistema.stock.model.Lote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository {

	Lote save(Lote lote);

	Optional<Lote> findById(Long id);

	List<Lote> findByItemId(Long itemId);

	List<Lote> findByProveedorId(Long proveedorId);

	List<Lote> findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(LocalDate fecha);

	List<Lote> findAll();
}
