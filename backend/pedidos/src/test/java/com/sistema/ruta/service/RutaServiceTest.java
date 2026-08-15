package com.sistema.ruta.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.in.GestionarRuta;
import com.sistema.ruta.port.out.PedidoGateway;
import com.sistema.ruta.port.out.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutaServiceTest {

	private RutaService rutaService;
	private FakeRutaRepository rutaRepository;
	private FakePedidoGateway pedidoGateway;
	private boolean repartidorValido;

	@BeforeEach
	void setUp() {
		rutaRepository = new FakeRutaRepository();
		pedidoGateway = new FakePedidoGateway();
		repartidorValido = true;
		rutaService = new RutaService(rutaRepository, pedidoGateway, zonaId -> true, usuarioId -> repartidorValido);
	}

	private GestionarRuta.CrearRutaCommand comandoConPedidos(Long... pedidos) {
		return new GestionarRuta.CrearRutaCommand(1L, 10L, LocalDate.of(2026, 8, 10), List.of(pedidos), null);
	}

	@Test
	void crearRutaAsignaPedidosDisponiblesYDeLaZona() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);

		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));

		assertEquals(EstadoRuta.PLANIFICADA, ruta.getEstado());
		assertEquals(1, ruta.getPedidoIds().size());
		assertTrue(ruta.getPedidoIds().contains(100L));
		assertTrue(pedidoGateway.llamadas.contains("ASIGNAR:100"));
	}

	@Test
	void crearRutaConPedidoNoDisponibleLanzaBusinessException() {
		pedidoGateway.disponible.put(100L, false);
		pedidoGateway.zonaDeCliente.put(100L, 1L);

		assertThrows(BusinessException.class, () -> rutaService.crearRuta(comandoConPedidos(100L)));
	}

	@Test
	void crearRutaConPedidoDeOtraZonaLanzaBusinessException() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 2L);

		assertThrows(BusinessException.class, () -> rutaService.crearRuta(comandoConPedidos(100L)));
	}

	@Test
	void crearRutaSinPedidosLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> rutaService.crearRuta(comandoConPedidos()));
	}

	@Test
	void crearRutaConRepartidorInvalidoLanzaBusinessException() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		repartidorValido = false;

		assertThrows(BusinessException.class, () -> rutaService.crearRuta(comandoConPedidos(100L)));
	}

	@Test
	void crearRutaConPedidoInexistenteLanzaNotFoundException() {
		// pedido 999 nunca registrado en el gateway
		assertThrows(NotFoundException.class, () -> rutaService.crearRuta(comandoConPedidos(999L)));
	}

	@Test
	void iniciarJornadaPasaPedidosAEnViaje() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));

		Ruta enCurso = rutaService.iniciarJornada(ruta.getId());

		assertEquals(EstadoRuta.EN_CURSO, enCurso.getEstado());
		assertTrue(pedidoGateway.llamadas.contains("VIAJE:100"));
	}

	@Test
	void iniciarJornadaSoloDesdePlanificada() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));
		rutaService.iniciarJornada(ruta.getId());

		assertThrows(BusinessException.class, () -> rutaService.iniciarJornada(ruta.getId()));
	}

	@Test
	void cerrarJornadaSoloDesdeEnCurso() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));

		assertThrows(BusinessException.class, () -> rutaService.cerrarJornada(ruta.getId()));

		rutaService.iniciarJornada(ruta.getId());
		pedidoGateway.enViaje.put(100L, false);
		Ruta cerrada = rutaService.cerrarJornada(ruta.getId());
		assertEquals(EstadoRuta.FINALIZADA, cerrada.getEstado());
	}

	@Test
	void cerrarJornadaConPedidoEnViajeLanzaBusinessException() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));
		rutaService.iniciarJornada(ruta.getId());
		pedidoGateway.enViaje.put(100L, true);

		BusinessException ex = assertThrows(BusinessException.class, () -> rutaService.cerrarJornada(ruta.getId()));
		assertTrue(ex.getMessage().contains("PED-100"), "El mensaje debe listar el número del pedido en viaje");
	}

	@Test
	void cerrarJornadaConTodosEntregadosCierra() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		pedidoGateway.disponible.put(200L, true);
		pedidoGateway.zonaDeCliente.put(200L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L, 200L));
		rutaService.iniciarJornada(ruta.getId());
		pedidoGateway.enViaje.put(100L, false);
		pedidoGateway.enViaje.put(200L, false);

		Ruta cerrada = rutaService.cerrarJornada(ruta.getId());
		assertEquals(EstadoRuta.FINALIZADA, cerrada.getEstado());
	}

	@Test
	void asignarPedidosAdicionalesSoloEnPlanificada() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		Ruta ruta = rutaService.crearRuta(comandoConPedidos(100L));
		rutaService.iniciarJornada(ruta.getId());

		assertThrows(BusinessException.class,
				() -> rutaService.asignarPedidos(ruta.getId(), List.of(200L)));
	}

	@Test
	void crearRutaConCapacidadSuficienteOk() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		pedidoGateway.unidades.put(100L, new BigDecimal("10"));
		Ruta ruta = rutaService.crearRuta(new GestionarRuta.CrearRutaCommand(1L, 10L,
				LocalDate.of(2026, 8, 10), List.of(100L), new BigDecimal("50")));
		assertEquals(0, new BigDecimal("50").compareTo(ruta.getCapacidadBultos()));
	}

	@Test
	void crearRutaExcedeCapacidadLanzaBusinessException() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		pedidoGateway.unidades.put(100L, new BigDecimal("60"));
		assertThrows(BusinessException.class, () -> rutaService.crearRuta(new GestionarRuta.CrearRutaCommand(
				1L, 10L, LocalDate.of(2026, 8, 10), List.of(100L), new BigDecimal("50"))));
	}

	@Test
	void asignarPedidosExcedeCapacidadLanza() {
		pedidoGateway.disponible.put(100L, true);
		pedidoGateway.zonaDeCliente.put(100L, 1L);
		pedidoGateway.unidades.put(100L, new BigDecimal("10"));
		Ruta ruta = rutaService.crearRuta(new GestionarRuta.CrearRutaCommand(1L, 10L,
				LocalDate.of(2026, 8, 10), List.of(100L), new BigDecimal("10")));
		pedidoGateway.disponible.put(200L, true);
		pedidoGateway.zonaDeCliente.put(200L, 1L);
		pedidoGateway.unidades.put(200L, new BigDecimal("10"));

		assertThrows(BusinessException.class, () -> rutaService.asignarPedidos(ruta.getId(), List.of(200L)));
	}

	private static class FakeRutaRepository implements RutaRepository {

		private final Map<Long, Ruta> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Ruta save(Ruta ruta) {
			if (ruta.getId() == null) {
				ruta.setId(secuencia.getAndIncrement());
			}
			datos.put(ruta.getId(), ruta);
			return ruta;
		}

		@Override
		public Optional<Ruta> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Ruta> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public List<Ruta> findByFechaJornada(LocalDate fechaJornada) {
			return datos.values().stream().filter(r -> r.getFechaJornada().equals(fechaJornada)).toList();
		}

		@Override
		public List<Ruta> findByRepartidorId(Long repartidorId) {
			return datos.values().stream().filter(r -> r.getRepartidorId().equals(repartidorId)).toList();
		}

		@Override
		public List<Ruta> findByEstado(EstadoRuta estado) {
			return datos.values().stream().filter(r -> r.getEstado() == estado).toList();
		}
	}

	private static class FakePedidoGateway implements PedidoGateway {

		private final Map<Long, Boolean> disponible = new HashMap<>();
		private final Map<Long, Long> zonaDeCliente = new HashMap<>();
		private final Map<Long, BigDecimal> unidades = new HashMap<>();
		private final Map<Long, Boolean> enViaje = new HashMap<>();
		private final List<String> llamadas = new ArrayList<>();

		@Override
		public BigDecimal unidadesDe(Long pedidoId) {
			return unidades.getOrDefault(pedidoId, BigDecimal.ONE);
		}

		@Override
		public String numeroDePedido(Long pedidoId) {
			return "PED-" + pedidoId;
		}

		@Override
		public boolean existePedido(Long pedidoId) {
			return disponible.containsKey(pedidoId) || zonaDeCliente.containsKey(pedidoId);
		}

		@Override
		public boolean estaDisponibleParaRuta(Long pedidoId) {
			return disponible.getOrDefault(pedidoId, false);
		}

		@Override
		public boolean clientePerteneceAZona(Long pedidoId, Long zonaId) {
			Long zona = zonaDeCliente.get(pedidoId);
			return zona != null && zona.equals(zonaId);
		}

		@Override
		public void asignarARuta(Long pedidoId) {
			llamadas.add("ASIGNAR:" + pedidoId);
		}

		@Override
		public void iniciarViaje(Long pedidoId) {
			llamadas.add("VIAJE:" + pedidoId);
		}

		@Override
		public boolean estaEnViaje(Long pedidoId) {
			return enViaje.getOrDefault(pedidoId, false);
		}
	}
}
