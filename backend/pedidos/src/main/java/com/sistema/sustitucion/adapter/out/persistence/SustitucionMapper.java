package com.sistema.sustitucion.adapter.out.persistence;

import com.sistema.sustitucion.model.Sustitucion;

public class SustitucionMapper {

	public Sustitucion toDomain(SustitucionJpaEntity entity) {
		Sustitucion s = new Sustitucion(entity.getPedidoId(), entity.getItemOriginalId(),
				entity.getItemSustitutoId(), entity.getCantidad(), entity.getDiferenciaPrecio(), entity.getFecha(),
				entity.getObservaciones());
		s.setId(entity.getId());
		return s;
	}

	public SustitucionJpaEntity toJpa(Sustitucion s) {
		SustitucionJpaEntity entity = new SustitucionJpaEntity(s.getPedidoId(), s.getItemOriginalId(),
				s.getItemSustitutoId(), s.getCantidad(), s.getDiferenciaPrecio(), s.getFecha(), s.getObservaciones());
		if (s.getId() != null) {
			entity.setId(s.getId());
		}
		return entity;
	}
}
