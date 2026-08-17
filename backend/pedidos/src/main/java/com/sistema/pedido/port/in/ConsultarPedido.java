package com.sistema.pedido.port.in;

import com.sistema.common.model.PageResponse;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ConsultarPedido {

	Optional<Pedido> buscarPorId(Long id);

	List<Pedido> listarTodos();

	List<Pedido> listarPorEstado(EstadoPedido estado);

	List<Pedido> listarPorCliente(Long clienteId);

	List<Pedido> listarPorVendedor(Long vendedorId);

	List<Pedido> listarHijosDe(Long pedidoPadreId);

	List<Pedido> listarPorIds(Collection<Long> ids);

	Map<EstadoPedido, Long> contadores();

	PageResponse<Pedido> listarPaginado(EstadoPedido estado, Long clienteId, Long vendedorId, int page, int size);

	PageResponse<Pedido> listarPaginadoPorEstadoYFecha(EstadoPedido estado, LocalDate fechaJornada, int page, int size);
}
