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

import java.math.BigDecimal;
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
			throw new NotFoundException("Zona no encontrada: " + command.zonaId());
		}
		if (!repartidorGateway.existeRepartidor(command.repartidorId())) {
			throw new BusinessException("REPARTIDOR_INVALIDO", "El usuario no es un REPARTIDOR activo");
		}
		if (command.fechaJornada() == null) {
			throw new BusinessException("VALIDATION_ERROR", "La fecha de jornada es obligatoria");
		}
		if (command.pedidoIds() == null || command.pedidoIds().isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "Una ruta debe tener al menos un pedido");
		}
		BigDecimal capacidad = command.capacidadBultos() == null ? BigDecimal.ZERO : command.capacidadBultos();
		if (capacidad.signum() < 0) {
			throw new BusinessException("VALIDATION_ERROR", "La capacidad no puede ser negativa");
		}
		Ruta ruta = new Ruta(command.zonaId(), command.repartidorId(), command.fechaJornada());
		ruta.setCapacidadBultos(capacidad);
		List<Long> sinDuplicados = command.pedidoIds().stream().distinct().toList();
		validarCapacidad(ruta, sinDuplicados);
		asignarPedidosValidados(sinDuplicados, command.zonaId());
		ruta.asignarPedidos(sinDuplicados);
		return rutaRepository.save(ruta);
	}

	@Override
	@Transactional
	public Ruta asignarPedidos(Long rutaId, List<Long> pedidoIds) {
		Ruta ruta = obtenerO404(rutaId);
		if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Solo se pueden asignar pedidos a una ruta PLANIFICADA");
		}
		List<Long> sinDuplicados = pedidoIds.stream().distinct()
				.filter(id -> !ruta.getPedidoIds().contains(id))
				.toList();
		validarCapacidad(ruta, sinDuplicados);
		asignarPedidosValidados(sinDuplicados, ruta.getZonaId());
		ruta.asignarPedidos(sinDuplicados);
		return rutaRepository.save(ruta);
	}

	private void asignarPedidosValidados(List<Long> pedidoIds, Long zonaId) {
		for (Long pedidoId : pedidoIds) {
			String numero = pedidoGateway.numeroDePedido(pedidoId);
			if (!pedidoGateway.existePedido(pedidoId)) {
				throw new NotFoundException("El pedido " + numero + " no existe");
			}
			if (!pedidoGateway.estaDisponibleParaRuta(pedidoId)) {
				throw new BusinessException("PEDIDO_NO_DISPONIBLE",
						"El pedido " + numero + " no está disponible para una ruta");
			}
			if (!pedidoGateway.clientePerteneceAZona(pedidoId, zonaId)) {
				throw new BusinessException("PEDIDO_ZONA_INCOMPATIBLE",
						"El pedido " + numero + " pertenece a un cliente fuera de la zona " + zonaId);
			}
			pedidoGateway.asignarARuta(pedidoId);
		}
	}

	private void validarCapacidad(Ruta ruta, List<Long> nuevos) {
		if (ruta.getCapacidadBultos().signum() <= 0) {
			return; // sin límite
		}
		java.util.List<Long> prospectivos = new java.util.ArrayList<>(ruta.getPedidoIds());
		prospectivos.addAll(nuevos);
		BigDecimal carga = BigDecimal.ZERO;
		for (Long pedidoId : prospectivos.stream().distinct().toList()) {
			carga = carga.add(pedidoGateway.unidadesDe(pedidoId));
		}
		if (carga.compareTo(ruta.getCapacidadBultos()) > 0) {
			throw new BusinessException("RUTA_CAPACIDAD_EXCEDIDA",
					"La ruta excede su capacidad: " + carga + " de " + ruta.getCapacidadBultos() + " bultos");
		}
	}

	@Override
	@Transactional
	public Ruta iniciarJornada(Long rutaId) {
		Ruta ruta = obtenerO404(rutaId);
		if (ruta.getEstado() != EstadoRuta.PLANIFICADA) {
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Solo las rutas PLANIFICADAS pueden iniciar");
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
			throw new BusinessException("RUTA_ESTADO_INVALIDO", "Solo las rutas EN_CURSO pueden cerrarse");
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
				.orElseThrow(() -> new NotFoundException("Ruta no encontrada: " + rutaId));
	}
}
