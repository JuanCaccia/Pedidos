package com.sistema.pedido.adapter.out.persistence;

import com.sistema.pedido.model.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {

	List<PedidoJpaEntity> findByEstado(EstadoPedido estado);

	List<PedidoJpaEntity> findByClienteId(Long clienteId);

	List<PedidoJpaEntity> findByVendedorId(Long vendedorId);

	List<PedidoJpaEntity> findByPedidoPadreId(Long pedidoPadreId);
}
