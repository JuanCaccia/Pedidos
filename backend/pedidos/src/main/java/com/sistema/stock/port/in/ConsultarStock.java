package com.sistema.stock.port.in;

import com.sistema.common.model.PageResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.model.MovimientoStock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ConsultarStock {

	Optional<Item> buscarItemPorId(Long id);

	List<Item> listarItems();

	PageResponse<Item> listarItemsPaginado(String q, String categoria, int page, int size);

	List<String> listarCategorias();

	BigDecimal obtenerDisponible(Long itemId);

	BigDecimal obtenerReservasActivas(Long itemId);

	List<MovimientoStock> listarMovimientos(Long itemId);

	PageResponse<MovimientoStock> listarMovimientosPaginado(Long itemId, int page, int size);

	List<Lote> listarLotes(Long itemId);

	List<Lote> listarLotesPorVencer(int dias);
}
