package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.cobranza.model.Cobranza;

public class CobranzaMapper {

	public Cobranza toDomain(CobranzaJpaEntity entity) {
		Cobranza cobranza = new Cobranza(entity.getClienteId(), entity.getPedidoId(), entity.getMonto(),
				entity.getFormaPago(), entity.getFecha(), entity.getObservaciones());
		cobranza.setId(entity.getId());
		return cobranza;
	}

	public CobranzaJpaEntity toJpa(Cobranza cobranza) {
		CobranzaJpaEntity entity = new CobranzaJpaEntity(cobranza.getClienteId(), cobranza.getPedidoId(),
				cobranza.getMonto(), cobranza.getFormaPago(), cobranza.getFecha(), cobranza.getObservaciones());
		if (cobranza.getId() != null) {
			entity.setId(cobranza.getId());
		}
		return entity;
	}
}
