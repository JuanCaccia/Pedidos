package com.sistema.cliente.port.in;

import com.sistema.cliente.model.Cliente;

public interface GestionarCliente {

	record CrearClienteCommand(String razonSocial, String cuit, String email, String telefono, String domicilio, Long zonaId) {
	}

	record ActualizarClienteCommand(Long clienteId, String razonSocial, String email, String telefono, String domicilio, Long zonaId) {
	}

	Cliente crearCliente(CrearClienteCommand command);

	Cliente actualizarCliente(ActualizarClienteCommand command);

	void desactivarCliente(Long clienteId);

	void reactivarCliente(Long clienteId);
}
