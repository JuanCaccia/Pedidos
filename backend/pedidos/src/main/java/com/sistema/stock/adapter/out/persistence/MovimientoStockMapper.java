package com.sistema.stock.adapter.out.persistence;

import com.sistema.stock.model.MovimientoStock;

public class MovimientoStockMapper {

	public MovimientoStock toDomain(MovimientoStockJpaEntity entity) {
		MovimientoStock movimiento = new MovimientoStock(entity.getTipo(), entity.getItemId(), entity.getLoteId(),
				entity.getPedidoId(), entity.getCantidad(), entity.getFecha(), entity.getMotivo());
		movimiento.setId(entity.getId());
		return movimiento;
	}

	public MovimientoStockJpaEntity toJpa(MovimientoStock movimiento) {
		MovimientoStockJpaEntity entity = new MovimientoStockJpaEntity(movimiento.getTipo(), movimiento.getItemId(),
				movimiento.getLoteId(), movimiento.getPedidoId(), movimiento.getCantidad(), movimiento.getFecha(),
				movimiento.getMotivo());
		if (movimiento.getId() != null) {
			entity.setId(movimiento.getId());
		}
		return entity;
	}
}
