package com.sistema.stock.adapter.out.persistence;

import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.port.out.MovimientoStockRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MovimientoStockRepositoryAdapter implements MovimientoStockRepository {

	private final MovimientoStockJpaRepository jpaRepository;
	private final MovimientoStockMapper mapper = new MovimientoStockMapper();

	public MovimientoStockRepositoryAdapter(MovimientoStockJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public MovimientoStock save(MovimientoStock movimiento) {
		MovimientoStockJpaEntity entity = mapper.toJpa(movimiento);
		MovimientoStockJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<MovimientoStock> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<MovimientoStock> findByItemIdOrderByFechaAsc(Long itemId) {
		return jpaRepository.findByItemIdOrderByFechaAsc(itemId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<MovimientoStock> findByLoteId(Long loteId) {
		return jpaRepository.findByLoteId(loteId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<MovimientoStock> findByPedidoId(Long pedidoId) {
		return jpaRepository.findByPedidoId(pedidoId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public PageResponse<MovimientoStock> listarPaginado(Long itemId, int page, int size) {
		return PageMapper.of(jpaRepository.findByItemIdOrderByFechaAsc(itemId, PageRequest.of(page, size)), mapper::toDomain);
	}
}
