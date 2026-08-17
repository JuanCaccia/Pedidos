package com.sistema.compra.adapter.out.persistence;

import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;
import com.sistema.compra.port.out.ProveedorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProveedorRepositoryAdapter implements ProveedorRepository {

	private final ProveedorJpaRepository jpaRepository;
	private final ProveedorItemJpaRepository proveedorItemJpaRepository;
	private final ProveedorMapper mapper = new ProveedorMapper();

	public ProveedorRepositoryAdapter(ProveedorJpaRepository jpaRepository,
			ProveedorItemJpaRepository proveedorItemJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.proveedorItemJpaRepository = proveedorItemJpaRepository;
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

	@Override
	public void reemplazarItems(Long proveedorId, List<Long> itemIds) {
		proveedorItemJpaRepository.deleteByProveedorId(proveedorId);
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		List<ProveedorItemJpaEntity> aGuardar = itemIds.stream()
				.distinct()
				.map(itemId -> new ProveedorItemJpaEntity(proveedorId, itemId))
				.toList();
		proveedorItemJpaRepository.saveAll(aGuardar);
	}

	@Override
	public List<ProveedorItem> listarItemsDeProveedor(Long proveedorId, boolean soloActivos) {
		return proveedorItemJpaRepository.listarItems(proveedorId, soloActivos).stream()
				.map(ProveedorRepositoryAdapter::toProveedorItem)
				.toList();
	}

	@Override
	public boolean proveedorProveeItemActivo(Long proveedorId, Long itemId) {
		return proveedorItemJpaRepository.existsByProveedorIdAndItemIdAndActivoTrue(proveedorId, itemId);
	}

	@Override
	public void vincularItem(Long proveedorId, Long itemId) {
		ProveedorItemId id = new ProveedorItemId(proveedorId, itemId);
		ProveedorItemJpaEntity entidad = proveedorItemJpaRepository.findById(id)
				.orElseGet(() -> new ProveedorItemJpaEntity(proveedorId, itemId));
		entidad.setActivo(true);
		proveedorItemJpaRepository.save(entidad);
	}

	@Override
	public List<Proveedor> listarProveedoresDeItem(Long itemId) {
		return jpaRepository.findProveedoresDeItemActivo(itemId).stream().map(mapper::toDomain).toList();
	}

	private static ProveedorItem toProveedorItem(Object[] fila) {
		ProveedorItem pi = new ProveedorItem((Long) fila[0], (Long) fila[1]);
		pi.setItemSku((String) fila[2]);
		pi.setItemNombre((String) fila[3]);
		pi.setActivo((Boolean) fila[4]);
		return pi;
	}
}
