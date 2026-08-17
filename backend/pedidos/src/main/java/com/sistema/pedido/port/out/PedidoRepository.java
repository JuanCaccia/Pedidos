package com.sistema.pedido.port.out;

import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository {

	Pedido save(Pedido pedido);

	Optional<Pedido> findById(Long id);

	List<Pedido> findAll();

	List<Pedido> findByEstado(EstadoPedido estado);

	long contarPorEstado(EstadoPedido estado);

	List<Pedido> findByClienteId(Long clienteId);

	List<Pedido> findByVendedorId(Long vendedorId);

	List<Pedido> findByPedidoPadreId(Long pedidoPadreId);

	List<Pedido> findByEstadoAndFechaJornada(EstadoPedido estado, LocalDate fechaJornada);

	List<Pedido> findByIds(Collection<Long> ids);
}
