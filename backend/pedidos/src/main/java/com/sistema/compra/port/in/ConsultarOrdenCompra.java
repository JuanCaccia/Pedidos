package com.sistema.compra.port.in;

import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;

import java.util.List;
import java.util.Optional;

public interface ConsultarOrdenCompra {

	Optional<OrdenCompra> buscarPorId(Long id);

	List<OrdenCompra> listarTodas();

	List<OrdenCompra> listarPorEstado(EstadoOrdenCompra estado);

	List<OrdenCompra> listarPorProveedor(Long proveedorId);
}
