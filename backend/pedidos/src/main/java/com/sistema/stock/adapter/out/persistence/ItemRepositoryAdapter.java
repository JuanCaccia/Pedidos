package com.sistema.stock.adapter.out.persistence;

import com.sistema.stock.model.Item;
import com.sistema.stock.port.out.ItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ItemRepositoryAdapter implements ItemRepository {

	private final ItemJpaRepository jpaRepository;
	private final ItemMapper mapper = new ItemMapper();

	public ItemRepositoryAdapter(ItemJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Item save(Item item) {
		ItemJpaEntity entity = mapper.toJpa(item);
		ItemJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Item> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Item> findBySku(String sku) {
		return jpaRepository.findBySku(sku).map(mapper::toDomain);
	}

	@Override
	public List<Item> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}
}
