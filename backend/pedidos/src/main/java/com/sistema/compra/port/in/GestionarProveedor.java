package com.sistema.compra.port.in;

import com.sistema.compra.model.Proveedor;

public interface GestionarProveedor {

	record CrearProveedorCommand(String razonSocial, String cuit, String email, String telefono) {
	}

	record ActualizarProveedorCommand(Long proveedorId, String razonSocial, String email, String telefono) {
	}

	Proveedor crearProveedor(CrearProveedorCommand command);

	Proveedor actualizarProveedor(ActualizarProveedorCommand command);

	void desactivarProveedor(Long proveedorId);

	void reactivarProveedor(Long proveedorId);
}
