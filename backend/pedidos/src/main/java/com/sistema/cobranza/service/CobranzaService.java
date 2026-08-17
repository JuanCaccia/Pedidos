package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.port.in.ConsultarCobranza;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.cobranza.port.out.ClienteGateway;
import com.sistema.cobranza.port.out.CobranzaRepository;
import com.sistema.cobranza.port.out.VentaGateway;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.usuario.model.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CobranzaService implements RegistrarCobranza, ConsultarCobranza {

	private static final Set<EstadoPedido> ESTADOS_COBRABLES = EnumSet.of(
			EstadoPedido.EN_VIAJE, EstadoPedido.ENTREGADO, EstadoPedido.ENTREGADO_PARCIAL);

	private final CobranzaRepository cobranzaRepository;
	private final ClienteGateway clienteGateway;
	private final VentaGateway ventaGateway;
	private final ConsultarPedido consultarPedido;
	private final ConsultarRuta consultarRuta;

	public CobranzaService(CobranzaRepository cobranzaRepository, ClienteGateway clienteGateway,
			VentaGateway ventaGateway, ConsultarPedido consultarPedido, ConsultarRuta consultarRuta) {
		this.cobranzaRepository = cobranzaRepository;
		this.clienteGateway = clienteGateway;
		this.ventaGateway = ventaGateway;
		this.consultarPedido = consultarPedido;
		this.consultarRuta = consultarRuta;
	}

	@Override
	@Transactional
	public Cobranza registrar(RegistrarCobranzaCommand command) {
		if (!clienteGateway.existeCliente(command.clienteId())) {
			throw new NotFoundException("Cliente no encontrado: " + command.clienteId());
		}
		if (command.monto() == null || command.monto().signum() == 0) {
			throw new BusinessException("VALIDATION_ERROR", "El monto no puede ser nulo ni cero");
		}
		if (command.formaPago() == null) {
			throw new BusinessException("VALIDATION_ERROR", "La forma de pago es obligatoria");
		}
		boolean esRepartidor = command.actor() != null && command.actor().tieneRol(Rol.REPARTIDOR);
		if (esRepartidor && command.pedidoId() == null) {
			throw new BusinessException("COBRANZA_REPARTIDOR_SIN_PEDIDO",
					"El repartidor solo puede registrar una cobranza asociada a un pedido de su ruta");
		}
		if (command.pedidoId() != null) {
			validarPedidoCobrable(command.pedidoId(), command.clienteId());
			if (esRepartidor && !consultarRuta.pedidoPerteneceARepartidor(command.actor().getId(), command.pedidoId())) {
				throw new BusinessException("COBRANZA_PEDIDO_NO_EN_RUTA",
						"El pedido " + command.pedidoId() + " no pertenece a una ruta del repartidor");
			}
		}
		Cobranza cobranza = new Cobranza(command.clienteId(), command.pedidoId(), command.monto(),
				command.formaPago(), LocalDateTime.now(), command.observaciones());
		return cobranzaRepository.save(cobranza);
	}

	private void validarPedidoCobrable(Long pedidoId, Long clienteId) {
		Pedido pedido = consultarPedido.buscarPorId(pedidoId)
				.orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + pedidoId));
		if (!pedido.getClienteId().equals(clienteId)) {
			throw new BusinessException("COBRANZA_PEDIDO_INVALIDO",
					"El pedido " + pedidoId + " no pertenece al cliente " + clienteId);
		}
		if (!ESTADOS_COBRABLES.contains(pedido.getEstado())) {
			throw new BusinessException("COBRANZA_PEDIDO_INVALIDO",
					"El pedido " + pedidoId + " está en estado " + pedido.getEstado() + " y no es cobrable");
		}
	}

	@Override
	public List<Cobranza> listar(Long clienteId, LocalDate desde, LocalDate hasta) {
		List<Cobranza> todas = clienteId != null ? cobranzaRepository.findByClienteId(clienteId) : cobranzaRepository.findAll();
		return todas.stream()
				.filter(c -> desde == null || !c.getFecha().toLocalDate().isBefore(desde))
				.filter(c -> hasta == null || !c.getFecha().toLocalDate().isAfter(hasta))
				.toList();
	}

	@Override
	public EstadoCuenta estadoCuenta(Long clienteId) {
		BigDecimal vendido = ventaGateway.totalVendidoCliente(clienteId);
		BigDecimal cobrado = cobranzaRepository.findByClienteId(clienteId).stream()
				.map(Cobranza::getMonto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new EstadoCuenta(clienteId, vendido, cobrado, vendido.subtract(cobrado));
	}
}
