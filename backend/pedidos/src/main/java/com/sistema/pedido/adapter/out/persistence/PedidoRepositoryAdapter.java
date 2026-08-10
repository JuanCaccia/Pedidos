package com.sistema.pedido.adapter.out.persistence;

import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.out.PedidoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PedidoRepositoryAdapter implements PedidoRepository {

	private final PedidoJpaRepository jpaRepository;
	private final PedidoMapper mapper = new PedidoMapper();

	public PedidoRepositoryAdapter(PedidoJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Pedido save(Pedido pedido) {
		PedidoJpaEntity entity = mapper.toJpa(pedido);
		PedidoJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Pedido> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Pedido> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Pedido> findByEstado(EstadoPedido estado) {
		return jpaRepository.findByEstado(estado).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Pedido> findByClienteId(Long clienteId) {
		return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Pedido> findByVendedorId(Long vendedorId) {
		return jpaRepository.findByVendedorId(vendedorId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Pedido> findByPedidoPadreId(Long pedidoPadreId) {
		return jpaRepository.findByPedidoPadreId(pedidoPadreId).stream().map(mapper::toDomain).toList();
	}
}
