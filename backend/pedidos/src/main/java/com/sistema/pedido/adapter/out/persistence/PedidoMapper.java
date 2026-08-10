package com.sistema.pedido.adapter.out.persistence;

import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.model.PedidoItem;

import java.util.ArrayList;

public class PedidoMapper {

	public Pedido toDomain(PedidoJpaEntity entity) {
		Pedido pedido = new Pedido(entity.getClienteId(), entity.getVendedorId(), entity.getFechaJornada(),
				entity.getObservaciones(), new ArrayList<>());
		pedido.setId(entity.getId());
		pedido.setNumero(entity.getNumero());
		pedido.setPedidoPadreId(entity.getPedidoPadreId());
		pedido.setEstado(entity.getEstado());
		pedido.setFechaCreacion(entity.getFechaCreacion());
		pedido.setTotal(entity.getTotal());
		for (PedidoItemJpaEntity lineaEntity : entity.getItems()) {
			PedidoItem linea = new PedidoItem(lineaEntity.getItemId(), lineaEntity.getCantidadPedida(),
					lineaEntity.getPrecioUnitario());
			linea.setId(lineaEntity.getId());
			linea.setCantidadReservada(lineaEntity.getCantidadReservada());
			linea.setCantidadEntregada(lineaEntity.getCantidadEntregada());
			linea.setPendienteStock(lineaEntity.isPendienteStock());
			pedido.agregarItem(linea);
		}
		return pedido;
	}

	public PedidoJpaEntity toJpa(Pedido pedido) {
		PedidoJpaEntity entity = new PedidoJpaEntity(pedido.getNumero(), pedido.getClienteId(),
				pedido.getVendedorId(), pedido.getPedidoPadreId(), pedido.getEstado(), pedido.getFechaCreacion(),
				pedido.getFechaJornada(), pedido.getObservaciones(), pedido.getTotal());
		if (pedido.getId() != null) {
			entity.setId(pedido.getId());
		}
		for (PedidoItem linea : pedido.getItems()) {
			PedidoItemJpaEntity lineaEntity = new PedidoItemJpaEntity(linea.getItemId(), linea.getCantidadPedida(),
					linea.getCantidadReservada(), linea.getCantidadEntregada(), linea.getPrecioUnitario(),
					linea.isPendienteStock());
			if (linea.getId() != null) {
				lineaEntity.setId(linea.getId());
			}
			entity.getItems().add(lineaEntity);
		}
		return entity;
	}
}
