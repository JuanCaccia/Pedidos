package com.sistema.stock.port.out;

import com.sistema.stock.model.MovimientoStock;

import java.util.List;
import java.util.Optional;

public interface MovimientoStockRepository {

	MovimientoStock save(MovimientoStock movimiento);

	Optional<MovimientoStock> findById(Long id);

	List<MovimientoStock> findByItemIdOrderByFechaAsc(Long itemId);

	List<MovimientoStock> findByLoteId(Long loteId);

	List<MovimientoStock> findByPedidoId(Long pedidoId);
}
