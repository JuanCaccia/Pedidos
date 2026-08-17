package com.sistema.pedido.adapter.out.persistence;

import com.sistema.pedido.model.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {

	List<PedidoJpaEntity> findByEstado(EstadoPedido estado);

	long countByEstado(EstadoPedido estado);

	List<PedidoJpaEntity> findByClienteId(Long clienteId);

	List<PedidoJpaEntity> findByVendedorId(Long vendedorId);

	List<PedidoJpaEntity> findByPedidoPadreId(Long pedidoPadreId);

	List<PedidoJpaEntity> findByEstadoAndFechaJornada(EstadoPedido estado, LocalDate fechaJornada);

	List<PedidoJpaEntity> findByIdIn(Collection<Long> ids);
}
