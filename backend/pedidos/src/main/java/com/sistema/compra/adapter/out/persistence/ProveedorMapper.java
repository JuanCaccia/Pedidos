package com.sistema.compra.adapter.out.persistence;

import com.sistema.compra.model.Proveedor;

public class ProveedorMapper {

	public Proveedor toDomain(ProveedorJpaEntity entity) {
		Proveedor proveedor = new Proveedor(entity.getRazonSocial(), entity.getCuit());
		proveedor.setId(entity.getId());
		proveedor.setEmail(entity.getEmail());
		proveedor.setTelefono(entity.getTelefono());
		proveedor.setActivo(entity.isActivo());
		return proveedor;
	}

	public ProveedorJpaEntity toJpa(Proveedor proveedor) {
		ProveedorJpaEntity entity = new ProveedorJpaEntity(proveedor.getRazonSocial(), proveedor.getCuit());
		if (proveedor.getId() != null) {
			entity.setId(proveedor.getId());
		}
		entity.setEmail(proveedor.getEmail());
		entity.setTelefono(proveedor.getTelefono());
		entity.setActivo(proveedor.isActivo());
		return entity;
	}
}
