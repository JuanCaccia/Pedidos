package com.sistema.cobranza.port.in;

import com.sistema.cobranza.model.Remito;

import java.util.List;
import java.util.Optional;

public interface ConsultarRemito {

	Optional<Remito> buscarPorId(Long id);

	List<Remito> listar(Long pedidoId, Long clienteId);
}
