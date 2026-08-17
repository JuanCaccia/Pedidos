package com.sistema.compra.adapter.out.persistence;

import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.model.OrdenCompraLinea;

public class OrdenCompraMapper {

	public OrdenCompra toDomain(OrdenCompraJpaEntity entity) {
		OrdenCompra orden = new OrdenCompra(entity.getProveedorId(), entity.getObservaciones());
		orden.setId(entity.getId());
		orden.setNumero(entity.getNumero());
		orden.setFecha(entity.getFecha());
		orden.setEstado(entity.getEstado());
		for (OrdenCompraLineaJpaEntity lineaEntity : entity.getLineas()) {
			OrdenCompraLinea linea = new OrdenCompraLinea(lineaEntity.getItemId(), lineaEntity.getCantidadPedida());
			linea.setId(lineaEntity.getId());
			linea.setCantidadRecibida(lineaEntity.getCantidadRecibida());
			orden.agregarLinea(linea);
		}
		return orden;
	}

	public OrdenCompraJpaEntity toJpa(OrdenCompra orden) {
		OrdenCompraJpaEntity entity = new OrdenCompraJpaEntity(orden.getNumero(), orden.getProveedorId(),
				orden.getFecha(), orden.getEstado(), orden.getObservaciones());
		if (orden.getId() != null) {
			entity.setId(orden.getId());
		}
		for (OrdenCompraLinea linea : orden.getLineas()) {
			OrdenCompraLineaJpaEntity lineaEntity = new OrdenCompraLineaJpaEntity(linea.getItemId(),
					linea.getCantidadPedida(), linea.getCantidadRecibida());
			if (linea.getId() != null) {
				lineaEntity.setId(linea.getId());
			}
			entity.getLineas().add(lineaEntity);
		}
		return entity;
	}
}
