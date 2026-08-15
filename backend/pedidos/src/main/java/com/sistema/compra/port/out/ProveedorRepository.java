package com.sistema.compra.port.out;

import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository {

	Proveedor save(Proveedor proveedor);

	Optional<Proveedor> findById(Long id);

	Optional<Proveedor> findByCuit(String cuit);

	List<Proveedor> findAll();

	PageResponse<Proveedor> buscar(String q, int page, int size);
}
