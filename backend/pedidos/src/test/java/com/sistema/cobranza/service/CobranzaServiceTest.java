package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.model.FormaPago;
import com.sistema.cobranza.port.in.ConsultarCobranza;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.cobranza.port.out.CobranzaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobranzaServiceTest {

	private CobranzaService cobranzaService;
	private FakeCobranzaRepository cobranzaRepository;
	private FakeConsultarPedido consultarPedido;
	private FakeConsultarRuta consultarRuta;
	private Usuario repartidor;

	@BeforeEach
	void setUp() {
		cobranzaRepository = new FakeCobranzaRepository();
		consultarPedido = new FakeConsultarPedido();
		consultarRuta = new FakeConsultarRuta();
		// Pedido 10 cobrable por defecto para el cliente 1.
		Pedido pedido = new Pedido(1L, 1L, null, null, false, new ArrayList<>());
		pedido.setId(10L);
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		consultarPedido.put(pedido);
		// Ruta EN_CURSO del repartidor 1 que contiene el pedido 10.
		Ruta ruta = new Ruta(1L, 1L, LocalDate.now());
		ruta.setId(100L);
		ruta.setEstado(EstadoRuta.EN_CURSO);
		ruta.asignarPedidos(List.of(10L));
		consultarRuta.put(ruta);
		repartidor = new Usuario("Repartidor", "rep@test.com", "x", Set.of(Rol.REPARTIDOR));
		repartidor.setId(1L);
		cobranzaService = new CobranzaService(cobranzaRepository, id -> true,
				clienteId -> new BigDecimal("1000.00"), consultarPedido, consultarRuta);
	}

	private RegistrarCobranza.RegistrarCobranzaCommand comando(Long clienteId, Long pedidoId, BigDecimal monto,
			FormaPago formaPago, String obs, Usuario actor) {
		return new RegistrarCobranza.RegistrarCobranzaCommand(clienteId, pedidoId, monto, formaPago, obs, actor);
	}

	@Test
	void registrarCobranzaPersiste() {
		Cobranza cobranza = cobranzaService.registrar(comando(1L, 10L,
				new BigDecimal("300.00"), FormaPago.EFECTIVO, "Pago parcial", null));

		assertNotNull(cobranza.getId());
		assertEquals(1L, cobranza.getClienteId());
		assertEquals(FormaPago.EFECTIVO, cobranza.getFormaPago());
		assertNotNull(cobranza.getFecha());
		assertEquals(1, cobranzaRepository.findAll().size());
	}

	@Test
	void cobranzaConPedidoDeOtroClienteLanzaBusinessException() {
		Pedido otroCliente = new Pedido(2L, 1L, null, null, false, new ArrayList<>());
		otroCliente.setId(20L);
		otroCliente.setEstado(EstadoPedido.ENTREGADO);
		consultarPedido.put(otroCliente);

		assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				comando(1L, 20L, new BigDecimal("100.00"),
						FormaPago.EFECTIVO, null, null)));
	}

	@Test
	void cobranzaConPedidoNoCobrableLanzaBusinessException() {
		Pedido pendiente = new Pedido(1L, 1L, null, null, false, new ArrayList<>());
		pendiente.setId(30L);
		pendiente.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		consultarPedido.put(pendiente);

		assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				comando(1L, 30L, new BigDecimal("100.00"),
						FormaPago.TRANSFERENCIA, null, null)));
	}

	@Test
	void cobranzaConPedidoInexistenteLanzaNotFoundException() {
		assertThrows(NotFoundException.class, () -> cobranzaService.registrar(
				comando(1L, 999L, new BigDecimal("100.00"),
						FormaPago.EFECTIVO, null, null)));
	}

	@Test
	void cobranzaConPedidoValidoDeClientePersiste() {
		Cobranza cobranza = cobranzaService.registrar(comando(1L, 10L,
				new BigDecimal("50.00"), FormaPago.EFECTIVO, "pago", null));

		assertNotNull(cobranza.getId());
		assertEquals(10L, cobranza.getPedidoId());
		assertEquals(1L, cobranza.getClienteId());
	}

	@Test
	void montoInvalidoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				comando(1L, 10L, BigDecimal.ZERO, FormaPago.EFECTIVO, null, null)));
	}

	@Test
	void montoCompensatorioNegativoPersiste() {
		// Sustitución con sustituto más caro => cobranza compensatoria negativa. El dominio
		// debe aceptar montos con signo (deuda que aumenta), solo rechaza nulo o cero.
		Cobranza cobranza = cobranzaService.registrar(comando(1L, 10L,
				new BigDecimal("-2.50"), FormaPago.OTRO, "Sustitución pedido 10", null));

		assertNotNull(cobranza.getId());
		assertEquals(0, new BigDecimal("-2.50").compareTo(cobranza.getMonto()));
	}

	@Test
	void clienteInexistenteLanzaNotFoundException() {
		CobranzaService servicio = new CobranzaService(cobranzaRepository, id -> false,
				clienteId -> BigDecimal.ZERO, consultarPedido, consultarRuta);

		assertThrows(NotFoundException.class, () -> servicio.registrar(
				comando(999L, null, new BigDecimal("100.00"),
						FormaPago.TRANSFERENCIA, null, null)));
	}

	@Test
	void repartidorCobraPedidoDeSuRuta() {
		Cobranza cobranza = cobranzaService.registrar(comando(1L, 10L,
				new BigDecimal("80.00"), FormaPago.EFECTIVO, null, repartidor));

		assertNotNull(cobranza.getId());
		assertEquals(10L, cobranza.getPedidoId());
	}

	@Test
	void repartidorSinPedidoLanzaCOBRANZA_REPARTIDOR_SIN_PEDIDO() {
		BusinessException ex = assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				comando(1L, null, new BigDecimal("80.00"), FormaPago.EFECTIVO, null, repartidor)));

		assertEquals("COBRANZA_REPARTIDOR_SIN_PEDIDO", ex.getCode());
	}

	@Test
	void repartidorConPedidoDeOtraRutaLanzaCOBRANZA_PEDIDO_NO_EN_RUTA() {
		Pedido deOtro = new Pedido(1L, 1L, null, null, false, new ArrayList<>());
		deOtro.setId(40L);
		deOtro.setEstado(EstadoPedido.EN_VIAJE);
		consultarPedido.put(deOtro);
		Ruta otra = new Ruta(1L, 2L, LocalDate.now());
		otra.setId(200L);
		otra.setEstado(EstadoRuta.EN_CURSO);
		otra.asignarPedidos(List.of(40L));
		consultarRuta.put(otra);

		BusinessException ex = assertThrows(BusinessException.class, () -> cobranzaService.registrar(
				comando(1L, 40L, new BigDecimal("80.00"), FormaPago.EFECTIVO, null, repartidor)));

		assertEquals("COBRANZA_PEDIDO_NO_EN_RUTA", ex.getCode());
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

	private static class FakeConsultarRuta implements ConsultarRuta {

		private final Map<Long, Ruta> datos = new HashMap<>();

		void put(Ruta ruta) {
			datos.put(ruta.getId(), ruta);
		}

		@Override
		public Optional<Ruta> buscarPorId(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Ruta> listarTodos() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public List<Ruta> listarPorFecha(LocalDate fechaJornada) {
			return datos.values().stream().filter(r -> r.getFechaJornada().equals(fechaJornada)).toList();
		}

		@Override
		public List<Ruta> listarPorRepartidor(Long repartidorId) {
			return datos.values().stream().filter(r -> r.getRepartidorId().equals(repartidorId)).toList();
		}

		@Override
		public List<Ruta> listarPorEstado(EstadoRuta estado) {
			return datos.values().stream().filter(r -> r.getEstado() == estado).toList();
		}

		@Override
		public boolean pedidoPerteneceARepartidor(Long repartidorId, Long pedidoId) {
			return listarPorRepartidor(repartidorId).stream()
					.anyMatch(r -> r.getEstado() != EstadoRuta.FINALIZADA && r.getPedidoIds().contains(pedidoId));
		}
	}

	private static class FakeConsultarPedido implements ConsultarPedido {

		private final Map<Long, Pedido> datos = new HashMap<>();

		void put(Pedido pedido) {
			datos.put(pedido.getId(), pedido);
		}

		@Override
		public Optional<Pedido> buscarPorId(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Pedido> listarTodos() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Pedido> listarPorEstado(EstadoPedido estado) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Pedido> listarPorCliente(Long clienteId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Pedido> listarPorVendedor(Long vendedorId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Pedido> listarHijosDe(Long pedidoPadreId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Pedido> listarPorIds(Collection<Long> ids) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<EstadoPedido, Long> contadores() {
			throw new UnsupportedOperationException();
		}

		@Override
		public com.sistema.common.model.PageResponse<Pedido> listarPaginado(EstadoPedido estado, Long clienteId,
				Long vendedorId, int page, int size) {
			throw new UnsupportedOperationException();
		}

		@Override
		public com.sistema.common.model.PageResponse<Pedido> listarPaginadoPorEstadoYFecha(EstadoPedido estado,
				LocalDate fechaJornada, int page, int size) {
			throw new UnsupportedOperationException();
		}
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
