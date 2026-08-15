package com.sistema.cobranza.port.out;

import com.sistema.cobranza.model.Remito;

import java.util.List;
import java.util.Optional;

public interface RemitoRepository {

	Remito save(Remito remito);

	Optional<Remito> findById(Long id);

	List<Remito> findByPedidoId(Long pedidoId);

	List<Remito> findByClienteId(Long clienteId);

	List<Remito> findAll();
}
