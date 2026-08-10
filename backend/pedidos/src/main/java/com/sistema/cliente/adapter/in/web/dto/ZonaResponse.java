package com.sistema.cliente.adapter.in.web.dto;

import com.sistema.common.model.Zona;

public record ZonaResponse(Long id, String nombre, boolean activo) {

	public static ZonaResponse from(Zona zona) {
		return new ZonaResponse(zona.getId(), zona.getNombre(), zona.isActivo());
	}
}
