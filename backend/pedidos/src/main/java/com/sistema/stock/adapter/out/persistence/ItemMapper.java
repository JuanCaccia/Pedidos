package com.sistema.stock.adapter.out.persistence;

import com.sistema.stock.model.Item;

public class ItemMapper {

	public Item toDomain(ItemJpaEntity entity) {
		Item item = new Item(entity.getSku(), entity.getNombre(), entity.getUnidadMedida());
		item.setId(entity.getId());
		item.setActivo(entity.isActivo());
		item.setStockMinimo(entity.getStockMinimo());
		item.setPrecioLista(entity.getPrecioLista());
		item.setCategoriaId(entity.getCategoriaId());
		return item;
	}

	public ItemJpaEntity toJpa(Item item) {
		ItemJpaEntity entity = new ItemJpaEntity(item.getSku(), item.getNombre(), item.getUnidadMedida());
		if (item.getId() != null) {
			entity.setId(item.getId());
		}
		entity.setActivo(item.isActivo());
		entity.setStockMinimo(item.getStockMinimo());
		entity.setPrecioLista(item.getPrecioLista());
		entity.setCategoriaId(item.getCategoriaId());
		return entity;
	}
}
