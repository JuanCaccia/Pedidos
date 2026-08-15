package com.sistema.compra.port.out;

import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository {

	OrdenCompra save(OrdenCompra ordenCompra);

	Optional<OrdenCompra> findById(Long id);

	List<OrdenCompra> findAll();

	List<OrdenCompra> findByEstado(EstadoOrdenCompra estado);

	List<OrdenCompra> findByProveedorId(Long proveedorId);
}
