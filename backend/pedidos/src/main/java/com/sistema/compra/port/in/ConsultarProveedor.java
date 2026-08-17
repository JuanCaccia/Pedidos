package com.sistema.compra.port.in;

import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;

import java.util.List;
import java.util.Optional;

public interface ConsultarProveedor {

	Optional<Proveedor> buscarPorId(Long id);

	List<Proveedor> listarTodos();

	PageResponse<Proveedor> listarPaginado(String q, int page, int size);

	List<ProveedorItem> listarItemsDeProveedor(Long proveedorId, boolean soloActivos);

	List<Proveedor> listarProveedoresDeItem(Long itemId);
}
