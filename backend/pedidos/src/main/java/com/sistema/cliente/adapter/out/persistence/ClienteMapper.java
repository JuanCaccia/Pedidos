package com.sistema.cliente.adapter.out.persistence;

import com.sistema.cliente.model.Cliente;

public class ClienteMapper {

	public Cliente toDomain(ClienteJpaEntity entity) {
		Cliente cliente = new Cliente(entity.getRazonSocial(), entity.getCuit(), entity.getZona());
		cliente.setId(entity.getId());
		cliente.setEmail(entity.getEmail());
		cliente.setTelefono(entity.getTelefono());
		cliente.setDomicilio(entity.getDomicilio());
		cliente.setActivo(entity.isActivo());
		return cliente;
	}

	public ClienteJpaEntity toJpa(Cliente cliente) {
		ClienteJpaEntity entity = new ClienteJpaEntity(cliente.getRazonSocial(), cliente.getCuit(), cliente.getZona());
		if (cliente.getId() != null) {
			entity.setId(cliente.getId());
		}
		entity.setEmail(cliente.getEmail());
		entity.setTelefono(cliente.getTelefono());
		entity.setDomicilio(cliente.getDomicilio());
		entity.setActivo(cliente.isActivo());
		return entity;
	}
}
