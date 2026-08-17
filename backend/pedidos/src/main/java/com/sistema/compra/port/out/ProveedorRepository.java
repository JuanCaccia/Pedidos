package com.sistema.compra.port.out;

import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository {

	Proveedor save(Proveedor proveedor);

	Optional<Proveedor> findById(Long id);

	Optional<Proveedor> findByCuit(String cuit);

	List<Proveedor> findAll();

	PageResponse<Proveedor> buscar(String q, int page, int size);

	/**
	 * Reemplaza por completo el catálogo de items activos de un proveedor.
	 * Los items no incluidos quedan desvinculados (vincular/desvincular).
	 */
	void reemplazarItems(Long proveedorId, List<Long> itemIds);

	List<ProveedorItem> listarItemsDeProveedor(Long proveedorId, boolean soloActivos);

	boolean proveedorProveeItemActivo(Long proveedorId, Long itemId);

	/**
	 * Vincula (activa) un item al catálogo del proveedor si aún no está vinculado.
	 * Operación idempotente: no elimina el resto del catálogo.
	 */
	void vincularItem(Long proveedorId, Long itemId);

	List<Proveedor> listarProveedoresDeItem(Long itemId);
}
