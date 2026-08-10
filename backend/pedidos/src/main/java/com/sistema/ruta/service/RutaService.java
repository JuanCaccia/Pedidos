package com.sistema.ruta.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.ruta.port.in.GestionarRuta;
import com.sistema.ruta.port.out.PedidoGateway;
import com.sistema.ruta.port.out.RepartidorGateway;
import com.sistema.ruta.port.out.RutaRepository;
import com.sistema.ruta.port.out.ZonaGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RutaService implements GestionarRuta, ConsultarRuta {

	private final RutaRepository rutaRepository;
	private final PedidoGateway pedidoGateway;
	private final ZonaGateway zonaGateway;
	private final RepartidorGateway repartidorGateway;

	public RutaService(RutaRepository rutaRepository, PedidoGateway pedidoGateway,
			ZonaGateway zonaGateway, RepartidorGateway repartidorGateway) {
		this.rutaRepository = rutaRepository;
		this.pedidoGateway = pedidoGateway;
		this.zonaGateway = zonaGateway;
		this.repartidorGateway = repartidorGateway;
	}

	@Override
	@Transactional
	public Ruta crearRuta(CrearRutaCommand command) {
		if (!zonaGateway.existeZona(command.zonaId())) {
			throw new NotFoundException("Zone not found: " + command.zonaId());
		}
		if (!repartidorGateway.existeRepartidor(command.repartidorId())) {
			throw new BusinessException("REPARTIDOR_INVALIDO", "The user is not an active REPARTIDOR");
		}
		if (command.fechaJornada() == null) {
			throw new BusinessException("VALIDATION_ERROR", "Journey date is required");
		}
		if (command.pedidoIds() == null || command.pedidoIds().isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "A route must have at least one order");
		}
		Ruta ruta = new Ruta(command.zonaId(), command.repartidorId(), command.fechaJornada());
		List<Long> sinDuplicados = command.pedidoIds().stream().distinct().toList();
		asignarPedidosValidados(sinDuplicados, command.zonaId());
		ruta.asignarPedidos(sinDuplicados);
		return rutaRepository.save(ruta);
	}

	@Override
	@Transactional
	public Ruta asignarPedidos(Long rutaId, List<Long> pedidoIds) {
		Ruta ruta = obtenerO404(rutaId);
		if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Orders can only be assigned to a PLANIFICADA route");
		}
		List<Long> sinDuplicados = pedidoIds.stream().distinct()
				.filter(id -> !ruta.getPedidoIds().contains(id))
				.toList();
		asignarPedidosValidados(sinDuplicados, ruta.getZonaId());
		ruta.asignarPedidos(sinDuplicados);
		return rutaRepository.save(ruta);
	}

	private void asignarPedidosValidados(List<Long> pedidoIds, Long zonaId) {
		for (Long pedidoId : pedidoIds) {
			if (!pedidoGateway.existePedido(pedidoId)) {
				throw new NotFoundException("Order not found: " + pedidoId);
			}
			if (!pedidoGateway.estaDisponibleParaRuta(pedidoId)) {
				throw new BusinessException("PEDIDO_NO_DISPONIBLE",
						"Order " + pedidoId + " is not available for a route");
			}
			if (!pedidoGateway.clientePerteneceAZona(pedidoId, zonaId)) {
				throw new BusinessException("PEDIDO_ZONA_INCOMPATIBLE",
						"Order " + pedidoId + " belongs to a client outside zone " + zonaId);
			}
			pedidoGateway.asignarARuta(pedidoId);
		}
	}

	@Override
	@Transactional
	public Ruta iniciarJornada(Long rutaId) {
		Ruta ruta = obtenerO404(rutaId);
		if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Only PLANIFICADA routes can start");
		}
		for (Long pedidoId : ruta.getPedidoIds()) {
			pedidoGateway.iniciarViaje(pedidoId);
		}
		ruta.iniciarJornada();
		return rutaRepository.save(ruta);
	}

	@Override
	@Transactional
	public Ruta cerrarJornada(Long rutaId) {
		Ruta ruta = obtenerO404(rutaId);
		if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Only EN_CURSO routes can be closed");
		}
		ruta.cerrarJornada();
		return rutaRepository.save(ruta);
	}

	@Override
	public Optional<Ruta> buscarPorId(Long id) {
		return rutaRepository.findById(id);
	}

	@Override
	public List<Ruta> listarTodos() {
		return rutaRepository.findAll();
	}

	@Override
	public List<Ruta> listarPorFecha(LocalDate fechaJornada) {
		return rutaRepository.findByFechaJornada(fechaJornada);
	}

	@Override
	public List<Ruta> listarPorRepartidor(Long repartidorId) {
		return rutaRepository.findByRepartidorId(repartidorId);
	}

	@Override
	public List<Ruta> listarPorEstado(EstadoRuta estado) {
		return rutaRepository.findByEstado(estado);
	}

	private Ruta obtenerO404(Long rutaId) {
		return rutaRepository.findById(rutaId)
				.orElseThrow(() -> new NotFoundException("Route not found: " + rutaId));
	}
}
