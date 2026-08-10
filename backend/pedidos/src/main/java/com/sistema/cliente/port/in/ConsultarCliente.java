package com.sistema.cliente.port.in;

import com.sistema.cliente.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ConsultarCliente {

	Optional<Cliente> buscarPorId(Long id);

	Optional<Cliente> buscarPorCuit(String cuit);

	List<Cliente> listarTodos();

	List<Cliente> listarPorZona(Long zonaId);
}
