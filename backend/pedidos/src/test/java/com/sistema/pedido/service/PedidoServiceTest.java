package com.sistema.pedido.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.CrearPedido;
import com.sistema.pedido.port.in.CrearPedido.CrearPedidoCommand;
import com.sistema.pedido.port.in.CrearPedido.LineaPedidoCommand;
import com.sistema.pedido.port.in.GestionarEntrega;
import com.sistema.pedido.port.in.GestionarEntrega.EntregaLineaCommand;
import com.sistema.pedido.port.in.GestionarEntrega.RegistrarEntregaCommand;
import com.sistema.pedido.port.out.ClienteGateway;
import com.sistema.pedido.port.out.PedidoRepository;
import com.sistema.pedido.port.out.StockGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedidoServiceTest {

	private PedidoService pedidoService;
	private FakePedidoRepository pedidoRepository;
	private FakeStockGateway stockGateway;

	@BeforeEach
	void setUp() {
		pedidoRepository = new FakePedidoRepository();
		stockGateway = new FakeStockGateway();
		pedidoService = new PedidoService(pedidoRepository, stockGateway, new ClienteGateway() {
			@Override
			public boolean existeCliente(Long clienteId) {
				return true;
			}

			@Override
			public Optional<Long> zonaDeCliente(Long clienteId) {
				return Optional.empty();
			}
		}, id -> true);
	}

	private Pedido crearPedido(Long itemId, BigDecimal cantidad) {
		return pedidoService.crearPedido(new CrearPedidoCommand(1L, 1L, null, null,
				List.of(new LineaPedidoCommand(itemId, cantidad, new BigDecimal("5.00")))));
	}

	@Test
	void crearPedidoPersisteConLineaYTotal() {
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		assertEquals(EstadoPedido.PENDIENTE_CONFIRMACION, pedido.getEstado());
		assertNotNull(pedido.getNumero());
		assertEquals(1, pedido.getItems().size());
		assertEquals(0, new BigDecimal("50.00").compareTo(pedido.getTotal()));
	}

	@Test
	void confirmarConStockSuficienteReservaTodo() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		Pedido confirmado = pedidoService.confirmarPedido(pedido.getId());

		assertEquals(EstadoPedido.PENDIENTE_PREPARACION, confirmado.getEstado());
		assertEquals(0, new BigDecimal("10.000").compareTo(confirmado.getItems().get(0).getCantidadReservada()));
		assertFalse(confirmado.getItems().get(0).isPendienteStock());
		assertEquals(0, new BigDecimal("90.000").compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void confirmarConStockParcialReservaLoDisponibleYMarcapendiente() {
		stockGateway.disponible.put(10L, new BigDecimal("6.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		Pedido confirmado = pedidoService.confirmarPedido(pedido.getId());

		assertEquals(EstadoPedido.PENDIENTE_PREPARACION, confirmado.getEstado());
		assertEquals(0, new BigDecimal("6.000").compareTo(confirmado.getItems().get(0).getCantidadReservada()));
		assertTrue(confirmado.getItems().get(0).isPendienteStock());
		assertEquals(0, BigDecimal.ZERO.compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void confirmarSinStockBloqueaYNoCambiaEstado() {
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		assertThrows(BusinessException.class, () -> pedidoService.confirmarPedido(pedido.getId()));

		Pedido recargado = pedidoRepository.findById(pedido.getId()).orElseThrow();
		assertEquals(EstadoPedido.PENDIENTE_CONFIRMACION, recargado.getEstado());
	}

	@Test
	void confirmarSoloPermitePendienteConfirmacion() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		assertThrows(BusinessException.class, () -> pedidoService.confirmarPedido(pedido.getId()));
	}

	@Test
	void entregaTotalCierraPedido() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);
		Long lineaId = pedido.getItems().get(0).getId();

		Pedido entregado = pedidoService.registrarEntrega(new RegistrarEntregaCommand(pedido.getId(),
				List.of(new EntregaLineaCommand(lineaId, new BigDecimal("10.000")))));

		assertEquals(EstadoPedido.ENTREGADO, entregado.getEstado());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("EGRESO:")));
		assertFalse(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("LIBERACION:")));
		assertEquals(0, pedidoRepository.findByPedidoPadreId(pedido.getId()).size());
	}

	@Test
	void entregaParcialCierraPadreGeneraHijoYLiberaSobrante() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);
		Long lineaId = pedido.getItems().get(0).getId();

		Pedido entregado = pedidoService.registrarEntrega(new RegistrarEntregaCommand(pedido.getId(),
				List.of(new EntregaLineaCommand(lineaId, new BigDecimal("8.000")))));

		assertEquals(EstadoPedido.ENTREGADO_PARCIAL, entregado.getEstado());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("EGRESO:")));
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("LIBERACION:")));

		List<Pedido> hijos = pedidoRepository.findByPedidoPadreId(pedido.getId());
		assertEquals(1, hijos.size());
		Pedido hijo = hijos.get(0);
		assertEquals(EstadoPedido.PENDIENTE_CONFIRMACION, hijo.getEstado());
		assertEquals(pedido.getId(), hijo.getPedidoPadreId());
		assertEquals(1, hijo.getItems().size());
		assertEquals(0, new BigDecimal("2.000").compareTo(hijo.getItems().get(0).getCantidadPedida()));
	}

	@Test
	void entregaExcedeReservaLanzaBusinessException() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);
		Long lineaId = pedido.getItems().get(0).getId();

		assertThrows(BusinessException.class, () -> pedidoService.registrarEntrega(new RegistrarEntregaCommand(
				pedido.getId(), List.of(new EntregaLineaCommand(lineaId, new BigDecimal("11.000"))))));
	}

	@Test
	void entregaVaciaLanzaBusinessException() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);
		Long lineaId = pedido.getItems().get(0).getId();

		assertThrows(BusinessException.class, () -> pedidoService.registrarEntrega(new RegistrarEntregaCommand(
				pedido.getId(), List.of(new EntregaLineaCommand(lineaId, BigDecimal.ZERO)))));
	}

	@Test
	void rechazarConReservaLibera() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		pedidoService.rechazarPedido(pedido.getId());

		Pedido recargado = pedidoRepository.findById(pedido.getId()).orElseThrow();
		assertEquals(EstadoPedido.RECHAZADO, recargado.getEstado());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("LIBERACION:")));
		assertEquals(0, new BigDecimal("100.000").compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void reAgendarMantieneReserva() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.PENDIENTE_ENTREGA);
		pedidoRepository.save(pedido);

		Pedido reagendado = pedidoService.reAgendar(pedido.getId());

		assertEquals(EstadoPedido.RE_AGENDADO, reagendado.getEstado());
		assertTrue(stockGateway.operaciones.stream().noneMatch(o -> o.startsWith("LIBERACION:")));
	}

	@Test
	void agregarUnidadesCompletaReserva() {
		stockGateway.disponible.put(10L, new BigDecimal("6.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		assertTrue(pedido.getItems().get(0).isPendienteStock());
		stockGateway.disponible.put(10L, new BigDecimal("4.000"));

		Pedido modificado = pedidoService.agregarUnidades(pedido.getId(), 10L, new BigDecimal("4.000"));

		assertFalse(modificado.getItems().get(0).isPendienteStock());
		assertEquals(0, new BigDecimal("10.000").compareTo(modificado.getItems().get(0).getCantidadReservada()));
		assertEquals(0, BigDecimal.ZERO.compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void agregarUnidadesSinPendienteOExcedenteLanza() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		assertThrows(BusinessException.class, () -> pedidoService.agregarUnidades(pedido.getId(), 10L, new BigDecimal("1.000")));

		stockGateway.disponible.put(11L, new BigDecimal("6.000"));
		Pedido pedido2 = crearPedido(11L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido2.getId());
		assertThrows(BusinessException.class, () -> pedidoService.agregarUnidades(pedido2.getId(), 11L, new BigDecimal("5.000")));
	}

	private static class FakePedidoRepository implements PedidoRepository {

		private final Map<Long, Pedido> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Pedido save(Pedido pedido) {
			if (pedido.getId() == null) {
				pedido.setId(secuencia.getAndIncrement());
			}
			datos.put(pedido.getId(), pedido);
			return pedido;
		}

		@Override
		public Optional<Pedido> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Pedido> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public List<Pedido> findByEstado(EstadoPedido estado) {
			return datos.values().stream().filter(p -> p.getEstado() == estado).toList();
		}

		@Override
		public List<Pedido> findByClienteId(Long clienteId) {
			return datos.values().stream().filter(p -> p.getClienteId().equals(clienteId)).toList();
		}

		@Override
		public List<Pedido> findByVendedorId(Long vendedorId) {
			return datos.values().stream().filter(p -> p.getVendedorId().equals(vendedorId)).toList();
		}

		@Override
		public List<Pedido> findByPedidoPadreId(Long pedidoPadreId) {
			return datos.values().stream().filter(p -> pedidoPadreId.equals(p.getPedidoPadreId())).toList();
		}
	}

	private static class FakeStockGateway implements StockGateway {

		private final Map<Long, BigDecimal> disponible = new HashMap<>();
		private final List<String> operaciones = new ArrayList<>();

		@Override
		public boolean existeItem(Long itemId) {
			return true;
		}

		@Override
		public BigDecimal consultarDisponible(Long itemId) {
			return disponible.getOrDefault(itemId, BigDecimal.ZERO);
		}

		@Override
		public void reservar(Long itemId, Long pedidoId, BigDecimal cantidad) {
			if (consultarDisponible(itemId).compareTo(cantidad) < 0) {
				throw new BusinessException("STOCK_INSUFICIENTE", "Not enough stock");
			}
			disponible.put(itemId, consultarDisponible(itemId).subtract(cantidad));
			operaciones.add("RESERVA:" + itemId + ":" + pedidoId + ":" + cantidad);
		}

		@Override
		public void liberarReserva(Long itemId, Long pedidoId, BigDecimal cantidad) {
			disponible.put(itemId, consultarDisponible(itemId).add(cantidad));
			operaciones.add("LIBERACION:" + itemId + ":" + pedidoId + ":" + cantidad);
		}

		@Override
		public void egresar(Long itemId, Long pedidoId, BigDecimal cantidad) {
			disponible.put(itemId, consultarDisponible(itemId).subtract(cantidad));
			operaciones.add("EGRESO:" + itemId + ":" + pedidoId + ":" + cantidad);
		}
	}
}
