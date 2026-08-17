package com.sistema.compra.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;
import com.sistema.compra.port.in.ConsultarProveedor;
import com.sistema.compra.port.in.GestionarProveedor;
import com.sistema.compra.port.out.ProveedorRepository;
import com.sistema.compra.port.out.StockGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProveedorService implements GestionarProveedor, ConsultarProveedor {

	private final ProveedorRepository proveedorRepository;
	private final StockGateway stockGateway;

	public ProveedorService(ProveedorRepository proveedorRepository, StockGateway stockGateway) {
		this.proveedorRepository = proveedorRepository;
		this.stockGateway = stockGateway;
	}

	@Override
	@Transactional
	public Proveedor crearProveedor(CrearProveedorCommand command) {
		if (command.razonSocial() == null || command.razonSocial().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "La razón social es obligatoria");
		}
		if (command.cuit() == null || !command.cuit().trim().matches("^\\d{11}$")) {
			throw new BusinessException("VALIDATION_ERROR", "El CUIT debe tener exactamente 11 dígitos");
		}
		String cuit = command.cuit().trim();
		proveedorRepository.findByCuit(cuit).ifPresent(p -> {
			throw new BusinessException("PROVEEDOR_CUIT_DUPLICADO", "Ya existe un proveedor con ese CUIT");
		});
		Proveedor proveedor = new Proveedor(command.razonSocial().trim(), cuit);
		proveedor.setEmail(command.email());
		proveedor.setTelefono(command.telefono());
		return proveedorRepository.save(proveedor);
	}

	@Override
	@Transactional
	public Proveedor actualizarProveedor(ActualizarProveedorCommand command) {
		Proveedor proveedor = proveedorRepository.findById(command.proveedorId())
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + command.proveedorId()));
		proveedor.actualizar(command.razonSocial().trim(), command.email(), command.telefono());
		return proveedorRepository.save(proveedor);
	}

	@Override
	@Transactional
	public void desactivarProveedor(Long proveedorId) {
		Proveedor proveedor = proveedorRepository.findById(proveedorId)
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + proveedorId));
		proveedor.desactivar();
		proveedorRepository.save(proveedor);
	}

	@Override
	@Transactional
	public void reactivarProveedor(Long proveedorId) {
		Proveedor proveedor = proveedorRepository.findById(proveedorId)
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + proveedorId));
		proveedor.reactivar();
		proveedorRepository.save(proveedor);
	}

	@Override
	public Optional<Proveedor> buscarPorId(Long id) {
		return proveedorRepository.findById(id);
	}

	@Override
	public List<Proveedor> listarTodos() {
		return proveedorRepository.findAll();
	}

	@Override
	public PageResponse<Proveedor> listarPaginado(String q, int page, int size) {
		return proveedorRepository.buscar(q, page, size);
	}

	@Override
	@Transactional
	public List<ProveedorItem> setItemsDeProveedor(SetItemsCommand command) {
		proveedorRepository.findById(command.proveedorId())
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + command.proveedorId()));
		List<Long> itemIds = command.itemIds() == null ? List.of() : command.itemIds();
		for (Long itemId : itemIds) {
			if (!stockGateway.existeItem(itemId)) {
				throw new NotFoundException("Item no encontrado: " + itemId);
			}
		}
		proveedorRepository.reemplazarItems(command.proveedorId(), itemIds);
		return proveedorRepository.listarItemsDeProveedor(command.proveedorId(), true);
	}

	@Override
	public List<ProveedorItem> listarItemsDeProveedor(Long proveedorId, boolean soloActivos) {
		proveedorRepository.findById(proveedorId)
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + proveedorId));
		return proveedorRepository.listarItemsDeProveedor(proveedorId, soloActivos);
	}

	@Override
	public List<Proveedor> listarProveedoresDeItem(Long itemId) {
		return proveedorRepository.listarProveedoresDeItem(itemId);
	}
}
