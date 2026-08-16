package com.sistema.stock.adapter.out.persistence;

import com.sistema.stock.model.Lote;

public class LoteMapper {

	public Lote toDomain(LoteJpaEntity entity) {
		Lote lote = new Lote(entity.getItemId(), entity.getCodigoLote(), entity.getFechaIngreso(),
				entity.getFechaVencimiento(), entity.getCantidadIngresada());
		lote.setId(entity.getId());
		lote.setProveedorId(entity.getProveedorId());
		lote.setEstado(entity.getEstado());
		return lote;
	}

	public LoteJpaEntity toJpa(Lote lote) {
		LoteJpaEntity entity = new LoteJpaEntity(lote.getItemId(), lote.getCodigoLote(), lote.getFechaIngreso(),
				lote.getFechaVencimiento(), lote.getCantidadIngresada());
		if (lote.getId() != null) {
			entity.setId(lote.getId());
		}
		entity.setProveedorId(lote.getProveedorId());
		entity.setEstado(lote.getEstado());
		return entity;
	}
}
