package com.sistema.cobranza.adapter.out.persistence;

import com.sistema.cobranza.model.Remito;
import com.sistema.cobranza.model.RemitoLinea;

public class RemitoMapper {

	public Remito toDomain(RemitoJpaEntity entity) {
		Remito remito = new Remito(entity.getPedidoId(), entity.getClienteId());
		remito.setId(entity.getId());
		remito.setNumero(entity.getNumero());
		remito.setFechaEmision(entity.getFechaEmision());
		remito.setMontoTotal(entity.getMontoTotal());
		for (RemitoLineaJpaEntity lineaEntity : entity.getLineas()) {
			RemitoLinea linea = new RemitoLinea(lineaEntity.getItemId(), lineaEntity.getCantidad(),
					lineaEntity.getPrecioUnitario());
			linea.setId(lineaEntity.getId());
			linea.setSubtotal(lineaEntity.getSubtotal());
			remito.getLineas().add(linea);
		}
		return remito;
	}

	public RemitoJpaEntity toJpa(Remito remito) {
		RemitoJpaEntity entity = new RemitoJpaEntity(remito.getNumero(), remito.getPedidoId(), remito.getClienteId(),
				remito.getFechaEmision(), remito.getMontoTotal());
		if (remito.getId() != null) {
			entity.setId(remito.getId());
		}
		for (RemitoLinea linea : remito.getLineas()) {
			RemitoLineaJpaEntity jpa = new RemitoLineaJpaEntity(linea.getItemId(), linea.getCantidad(),
					linea.getPrecioUnitario(), linea.getSubtotal());
			if (linea.getId() != null) {
				jpa.setId(linea.getId());
			}
			entity.getLineas().add(jpa);
		}
		return entity;
	}
}
