package com.sistema.compra.adapter.out.persistence;

import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.port.out.ProveedorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProveedorRepositoryAdapter implements ProveedorRepository {

	private final ProveedorJpaRepository jpaRepository;
	private final ProveedorMapper mapper = new ProveedorMapper();

	public ProveedorRepositoryAdapter(ProveedorJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Proveedor save(Proveedor proveedor) {
		ProveedorJpaEntity entity = mapper.toJpa(proveedor);
		ProveedorJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Proveedor> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Proveedor> findByCuit(String cuit) {
		return jpaRepository.findByCuit(cuit).map(mapper::toDomain);
	}

	@Override
	public List<Proveedor> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public PageResponse<Proveedor> buscar(String q, int page, int size) {
		return PageMapper.of(jpaRepository.buscar(normalizar(q), PageRequest.of(page, size)), mapper::toDomain);
	}

	private String normalizar(String q) {
		return q == null || q.isBlank() ? null : q.trim();
	}
}
