package com.sistema.compra.adapter.out.persistence;

import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.port.out.OrdenCompraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrdenCompraRepositoryAdapter implements OrdenCompraRepository {

	private final OrdenCompraJpaRepository jpaRepository;
	private final OrdenCompraMapper mapper = new OrdenCompraMapper();

	public OrdenCompraRepositoryAdapter(OrdenCompraJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public OrdenCompra save(OrdenCompra ordenCompra) {
		OrdenCompraJpaEntity entity = mapper.toJpa(ordenCompra);
		OrdenCompraJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<OrdenCompra> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<OrdenCompra> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<OrdenCompra> findByEstado(EstadoOrdenCompra estado) {
		return jpaRepository.findByEstado(estado).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<OrdenCompra> findByProveedorId(Long proveedorId) {
		return jpaRepository.findByProveedorId(proveedorId).stream().map(mapper::toDomain).toList();
	}
}
