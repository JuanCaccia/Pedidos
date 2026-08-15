package com.sistema.cobranza.port.out;

import com.sistema.cobranza.model.Cobranza;

import java.util.List;
import java.util.Optional;

public interface CobranzaRepository {

	Cobranza save(Cobranza cobranza);

	Optional<Cobranza> findById(Long id);

	List<Cobranza> findByClienteId(Long clienteId);

	List<Cobranza> findAll();
}
