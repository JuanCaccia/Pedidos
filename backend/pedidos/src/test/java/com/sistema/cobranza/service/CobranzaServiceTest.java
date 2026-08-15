package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.model.FormaPago;
import com.sistema.cobranza.port.in.ConsultarCobranza;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.cobranza.port.out.CobranzaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobranzaServiceTest {

	private CobranzaService cobranzaService;
	private FakeCobranzaRepository cobranzaRepository;

	@BeforeEach
	void setUp() {
		cobranzaRepository = new FakeCobranzaRepository();
		cobranzaService = new CobranzaService(cobranzaRepository, id -> true,
				clienteId -> new BigDecimal("1000.00"));
	}

	@Test
	void registrarCobranzaPersiste() {
		Cobranza cobranza = cobranzaService.registrar(new RegistrarCobranza.RegistrarCobranzaCommand(1L, 10L,
				new BigDecimal("300.00"), FormaPago.EFECTIVO, "Pago parcial"));

		assertNotNull(cobranza.getId());
		assertEquals(1L, cobranza.getClienteId());
		assertEquals(FormaPago.EFECTIVO, cobranza.getFormaPago());
		assertNotNull(cobranza.getFecha());
		assertEquals(1, cobranzaRepository.findAll().size());
	}

	@Test
	void montoInvalidoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				new RegistrarCobranza.RegistrarCobranzaCommand(1L, 10L, BigDecimal.ZERO, FormaPago.EFECTIVO, null)));
	}

	@Test
	void montoCompensatorioNegativoPersiste() {
		// Sustitución con sustituto más caro => cobranza compensatoria negativa. El dominio
		// debe aceptar montos con signo (deuda que aumenta), solo rechaza nulo o cero.
		Cobranza cobranza = cobranzaService.registrar(new RegistrarCobranza.RegistrarCobranzaCommand(1L, 10L,
				new BigDecimal("-2.50"), FormaPago.OTRO, "Sustitución pedido 10"));

		assertNotNull(cobranza.getId());
		assertEquals(0, new BigDecimal("-2.50").compareTo(cobranza.getMonto()));
	}

	@Test
	void clienteInexistenteLanzaNotFoundException() {
		CobranzaService servicio = new CobranzaService(cobranzaRepository, id -> false,
				clienteId -> BigDecimal.ZERO);

		assertThrows(NotFoundException.class, () -> servicio.registrar(
				new RegistrarCobranza.RegistrarCobranzaCommand(999L, null, new BigDecimal("100.00"),
						FormaPago.TRANSFERENCIA, null)));
	}

	@Test
	void estadoCuentaCalculaSaldo() {
		cobranzaRepository.save(new Cobranza(1L, null, new BigDecimal("300.00"), FormaPago.EFECTIVO,
				LocalDateTime.now(), null));

		ConsultarCobranza.EstadoCuenta cuenta = cobranzaService.estadoCuenta(1L);

		assertEquals(0, new BigDecimal("1000.00").compareTo(cuenta.totalVendido()));
		assertEquals(0, new BigDecimal("300.00").compareTo(cuenta.totalCobrado()));
		assertEquals(0, new BigDecimal("700.00").compareTo(cuenta.saldo()));
	}

	private static class FakeCobranzaRepository implements CobranzaRepository {

		private final Map<Long, Cobranza> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Cobranza save(Cobranza cobranza) {
			if (cobranza.getId() == null) {
				cobranza.setId(secuencia.getAndIncrement());
			}
			datos.put(cobranza.getId(), cobranza);
			return cobranza;
		}

		@Override
		public Optional<Cobranza> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Cobranza> findByClienteId(Long clienteId) {
			return datos.values().stream().filter(c -> c.getClienteId().equals(clienteId)).toList();
		}

		@Override
		public List<Cobranza> findAll() {
			return new ArrayList<>(datos.values());
		}
	}
}
