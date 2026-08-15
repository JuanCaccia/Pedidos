package com.sistema.ruta.adapter.out.persistence;

import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.model.RutaPedido;

public class RutaMapper {

	public Ruta toDomain(RutaJpaEntity entity) {
		Ruta ruta = new Ruta(entity.getZonaId(), entity.getRepartidorId(), entity.getFechaJornada());
		ruta.setId(entity.getId());
		ruta.setEstado(entity.getEstado());
		ruta.setCapacidadBultos(entity.getCapacidadBultos());
		for (RutaPedidoJpaEntity pedido : entity.getPedidos()) {
			RutaPedido rp = new RutaPedido(pedido.getPedidoId());
			rp.setId(pedido.getId());
			ruta.getPedidos().add(rp);
		}
		return ruta;
	}

	public RutaJpaEntity toJpa(Ruta ruta) {
		RutaJpaEntity entity = new RutaJpaEntity(ruta.getZonaId(), ruta.getRepartidorId(),
				ruta.getFechaJornada(), ruta.getEstado());
		if (ruta.getId() != null) {
			entity.setId(ruta.getId());
		}
		entity.setCapacidadBultos(ruta.getCapacidadBultos());
		for (RutaPedido rp : ruta.getPedidos()) {
			RutaPedidoJpaEntity jpa = new RutaPedidoJpaEntity(rp.getPedidoId());
			if (rp.getId() != null) {
				jpa.setId(rp.getId());
			}
			entity.getPedidos().add(jpa);
		}
		return entity;
	}
}
