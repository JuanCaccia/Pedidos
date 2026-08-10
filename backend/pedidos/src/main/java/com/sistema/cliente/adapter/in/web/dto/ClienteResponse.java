package com.sistema.cliente.adapter.in.web.dto;

import com.sistema.cliente.model.Cliente;

public record ClienteResponse(Long id, String razonSocial, String cuit, String email, String telefono,
		String domicilio, Long zonaId, String zonaNombre, boolean activo) {

	public static ClienteResponse from(Cliente cliente) {
		return new ClienteResponse(cliente.getId(), cliente.getRazonSocial(), cliente.getCuit(),
				cliente.getEmail(), cliente.getTelefono(), cliente.getDomicilio(),
				cliente.getZona() != null ? cliente.getZona().getId() : null,
				cliente.getZona() != null ? cliente.getZona().getNombre() : null,
				cliente.isActivo());
	}
}
