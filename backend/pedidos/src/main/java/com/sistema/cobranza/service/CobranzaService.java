package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.port.in.ConsultarCobranza;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.cobranza.port.out.ClienteGateway;
import com.sistema.cobranza.port.out.CobranzaRepository;
import com.sistema.cobranza.port.out.VentaGateway;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CobranzaService implements RegistrarCobranza, ConsultarCobranza {

	private final CobranzaRepository cobranzaRepository;
	private final ClienteGateway clienteGateway;
	private final VentaGateway ventaGateway;

	public CobranzaService(CobranzaRepository cobranzaRepository, ClienteGateway clienteGateway, VentaGateway ventaGateway) {
		this.cobranzaRepository = cobranzaRepository;
		this.clienteGateway = clienteGateway;
		this.ventaGateway = ventaGateway;
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
		Cobranza cobranza = new Cobranza(command.clienteId(), command.pedidoId(), command.monto(),
				command.formaPago(), LocalDateTime.now(), command.observaciones());
		return cobranzaRepository.save(cobranza);
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
