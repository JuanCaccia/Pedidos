package com.sistema.stock.port.out;

import com.sistema.common.model.PageResponse;
import com.sistema.stock.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

	Item save(Item item);

	Optional<Item> findById(Long id);

	Optional<Item> findBySku(String sku);

	List<Item> findAll();

	PageResponse<Item> buscar(String q, Long categoriaId, int page, int size);

	PageResponse<Item> buscarActivos(String q, Long categoriaId, int page, int size);
}
