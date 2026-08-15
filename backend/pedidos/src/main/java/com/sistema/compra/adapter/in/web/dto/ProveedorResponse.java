package com.sistema.compra.adapter.in.web.dto;

import com.sistema.compra.model.Proveedor;

public record ProveedorResponse(Long id, String razonSocial, String cuit, String email, String telefono,
		boolean activo) {

	public static ProveedorResponse from(Proveedor proveedor) {
		return new ProveedorResponse(proveedor.getId(), proveedor.getRazonSocial(), proveedor.getCuit(),
				proveedor.getEmail(), proveedor.getTelefono(), proveedor.isActivo());
	}
}
