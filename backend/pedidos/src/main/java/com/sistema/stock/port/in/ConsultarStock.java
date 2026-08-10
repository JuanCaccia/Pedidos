package com.sistema.stock.port.in;

import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ConsultarStock {

	Optional<Item> buscarItemPorId(Long id);

	List<Item> listarItems();

	BigDecimal obtenerDisponible(Long itemId);

	BigDecimal obtenerReservasActivas(Long itemId);

	List<MovimientoStock> listarMovimientos(Long itemId);

	List<Lote> listarLotes(Long itemId);
}
