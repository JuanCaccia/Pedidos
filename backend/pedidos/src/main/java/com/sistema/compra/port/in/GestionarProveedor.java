package com.sistema.compra.port.in;

import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;

import java.util.List;

public interface GestionarProveedor {

	record CrearProveedorCommand(String razonSocial, String cuit, String email, String telefono) {
	}

	record ActualizarProveedorCommand(Long proveedorId, String razonSocial, String email, String telefono) {
	}

	record SetItemsCommand(Long proveedorId, List<Long> itemIds) {
	}

	Proveedor crearProveedor(CrearProveedorCommand command);

	Proveedor actualizarProveedor(ActualizarProveedorCommand command);

	void desactivarProveedor(Long proveedorId);

	void reactivarProveedor(Long proveedorId);

	/**
	 * Reemplaza el catálogo completo de items activos del proveedor.
	 */
	List<ProveedorItem> setItemsDeProveedor(SetItemsCommand command);
}
