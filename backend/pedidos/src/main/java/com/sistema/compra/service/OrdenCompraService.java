package com.sistema.compra.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.model.OrdenCompraLinea;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.port.in.ConsultarOrdenCompra;
import com.sistema.compra.port.in.GestionarOrdenCompra;
import com.sistema.compra.port.out.OrdenCompraRepository;
import com.sistema.compra.port.out.ProveedorRepository;
import com.sistema.compra.port.out.StockGateway;
import com.sistema.stock.model.Lote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrdenCompraService implements GestionarOrdenCompra, ConsultarOrdenCompra {

	private final OrdenCompraRepository ordenCompraRepository;
	private final ProveedorRepository proveedorRepository;
	private final StockGateway stockGateway;

	public OrdenCompraService(OrdenCompraRepository ordenCompraRepository, ProveedorRepository proveedorRepository, StockGateway stockGateway) {
		this.ordenCompraRepository = ordenCompraRepository;
		this.proveedorRepository = proveedorRepository;
		this.stockGateway = stockGateway;
	}

	@Override
	@Transactional
	public OrdenCompra crearOrdenCompra(CrearOrdenCompraCommand command) {
		Proveedor proveedor = proveedorRepository.findById(command.proveedorId())
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + command.proveedorId()));
		if (!proveedor.isActivo()) {
			throw new BusinessException("PROVEEDOR_INACTIVO",
					"El proveedor " + command.proveedorId() + " está inactivo y no puede generar órdenes de compra");
		}
		if (command.lineas() == null || command.lineas().isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "La orden de compra debe tener al menos una línea");
		}
		OrdenCompra orden = new OrdenCompra(command.proveedorId(), command.observaciones());
		for (LineaOrdenCommand linea : command.lineas()) {
			if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "La cantidad debe ser mayor a cero");
			}
			if (!stockGateway.existeItem(linea.itemId())) {
				throw new NotFoundException("Item no encontrado: " + linea.itemId());
			}
				if (!stockGateway.itemActivo(linea.itemId())) {
					throw new BusinessException("ITEM_INACTIVO",
							"El item " + linea.itemId() + " está inactivo y no puede incluirse en una orden de compra");
				}
				if (!proveedorRepository.proveedorProveeItemActivo(command.proveedorId(), linea.itemId())) {
					throw new BusinessException("ITEM_NO_PROVISTO_POR_PROVEEDOR",
							"El item " + linea.itemId() + " no está en el catálogo activo del proveedor " + command.proveedorId());
				}
				orden.agregarLinea(new OrdenCompraLinea(linea.itemId(), linea.cantidad()));
		}
		orden.setNumero("OC-" + String.format("%06d", System.nanoTime() % 1000000));
		return ordenCompraRepository.save(orden);
	}

	@Override
	@Transactional
	public OrdenCompra registrarRecepcion(RecepcionCommand command) {
		OrdenCompra orden = obtenerO404(command.ordenId());
		if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE
				&& orden.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
			throw new BusinessException("OC_ESTADO_INVALIDO",
					"Solo se puede recibir una OC en PENDIENTE o RECIBIDA_PARCIAL");
		}
		for (RecepcionLineaCommand rl : command.lineas()) {
			OrdenCompraLinea linea = orden.lineaPorId(rl.lineaId())
					.orElseThrow(() -> new NotFoundException("Línea de OC no encontrada: " + rl.lineaId()));
			if (rl.cantidadRecibida() == null || rl.cantidadRecibida().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "La cantidad recibida debe ser mayor a cero");
			}
			if (rl.precioUnitario() == null || rl.precioUnitario().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "El precio unitario recibido debe ser mayor a cero");
			}
			BigDecimal restante = linea.restante();
			if (rl.cantidadRecibida().compareTo(restante) > 0) {
				throw new BusinessException("VALIDATION_ERROR", "No se puede recibir más que el restante (" + restante + ")");
			}
			linea.recibir(rl.cantidadRecibida());
			String codigoLote = orden.getNumero() + "-" + (System.nanoTime() % 100000);
			stockGateway.registrarIngreso(linea.getItemId(), codigoLote, rl.cantidadRecibida(),
					"Recepción de OC " + orden.getNumero(), orden.getProveedorId(), rl.precioUnitario());
			proveedorRepository.vincularItem(orden.getProveedorId(), linea.getItemId());
		}
		boolean completa = orden.getLineas().stream()
				.allMatch(l -> l.getCantidadRecibida().compareTo(l.getCantidadPedida()) >= 0);
		orden.setEstado(completa ? EstadoOrdenCompra.RECIBIDA : EstadoOrdenCompra.RECIBIDA_PARCIAL);
		return ordenCompraRepository.save(orden);
	}

	@Override
	@Transactional
	public List<Lote> registrarRecepcionCsv(RecepcionCsvCommand command) {
		OrdenCompra orden = obtenerO404(command.ordenId());
		if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE
				&& orden.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
			throw new BusinessException("OC_ESTADO_INVALIDO",
					"Solo se puede recibir una OC en PENDIENTE o RECIBIDA_PARCIAL");
		}
		List<Lote> lotes = new ArrayList<>();
		for (RecepcionCsvLineaCommand rl : command.lineas()) {
			OrdenCompraLinea linea = orden.lineaPorItemId(rl.itemId())
					.orElseThrow(() -> new BusinessException("ITEM_NO_EN_OC",
							"El item " + rl.itemId() + " no está en la orden de compra"));
			if (rl.cantidadRecibida() == null || rl.cantidadRecibida().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "La cantidad recibida debe ser mayor a cero");
			}
			if (rl.precioUnitario() == null || rl.precioUnitario().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "El precio unitario recibido debe ser mayor a cero");
			}
			BigDecimal restante = linea.restante();
			if (rl.cantidadRecibida().compareTo(restante) > 0) {
				throw new BusinessException("VALIDATION_ERROR", "No se puede recibir más que el restante (" + restante + ")");
			}
			linea.recibir(rl.cantidadRecibida());
			String codigoLote = rl.codigoLote() != null && !rl.codigoLote().isBlank()
					? rl.codigoLote()
					: orden.getNumero() + "-" + (System.nanoTime() % 100000);
			Lote lote = stockGateway.registrarIngresoConLote(linea.getItemId(), codigoLote, rl.fechaVencimiento(),
					rl.cantidadRecibida(), "Recepción de OC " + orden.getNumero(),
					orden.getProveedorId(), rl.precioUnitario());
			proveedorRepository.vincularItem(orden.getProveedorId(), linea.getItemId());
			lotes.add(lote);
		}
		boolean completa = orden.getLineas().stream()
				.allMatch(l -> l.getCantidadRecibida().compareTo(l.getCantidadPedida()) >= 0);
		orden.setEstado(completa ? EstadoOrdenCompra.RECIBIDA : EstadoOrdenCompra.RECIBIDA_PARCIAL);
		ordenCompraRepository.save(orden);
		return lotes;
	}

	@Override
	@Transactional
	public void cancelarOrdenCompra(Long ordenId) {		OrdenCompra orden = obtenerO404(ordenId);
		if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE
				&& orden.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
			throw new BusinessException("OC_ESTADO_INVALIDO", "Solo se puede cancelar una OC en PENDIENTE o RECIBIDA_PARCIAL");
		}
		orden.setEstado(EstadoOrdenCompra.CANCELADA);
		ordenCompraRepository.save(orden);
	}

	@Override
	public Optional<OrdenCompra> buscarPorId(Long id) {
		return ordenCompraRepository.findById(id);
	}

	@Override
	public List<OrdenCompra> listarTodas() {
		return ordenCompraRepository.findAll();
	}

	@Override
	public List<OrdenCompra> listarPorEstado(EstadoOrdenCompra estado) {
		return ordenCompraRepository.findByEstado(estado);
	}

	@Override
	public List<OrdenCompra> listarPorProveedor(Long proveedorId) {
		return ordenCompraRepository.findByProveedorId(proveedorId);
	}

	private OrdenCompra obtenerO404(Long ordenId) {
		return ordenCompraRepository.findById(ordenId)
				.orElseThrow(() -> new NotFoundException("Orden de compra no encontrada: " + ordenId));
	}
}
