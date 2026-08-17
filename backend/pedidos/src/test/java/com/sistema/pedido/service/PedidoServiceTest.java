package com.sistema.pedido.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.model.PedidoItem;
import com.sistema.pedido.port.in.CrearPedido;
import com.sistema.pedido.port.in.CrearPedido.CrearPedidoCommand;
import com.sistema.pedido.port.in.CrearPedido.LineaPedidoCommand;
import com.sistema.pedido.port.in.GestionarEntrega;
import com.sistema.pedido.port.in.GestionarEntrega.EntregaLineaCommand;
import com.sistema.pedido.port.in.GestionarEntrega.RegistrarEntregaCommand;
import com.sistema.pedido.port.in.ModificarStockPedido;
import com.sistema.pedido.port.out.ClienteGateway;
import com.sistema.pedido.port.out.NotificacionGateway;
import com.sistema.pedido.port.out.PedidoRepository;
import com.sistema.pedido.port.out.StockGateway;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	private boolean clienteInactivo;
	private final List<Long> remitosGenerados = new ArrayList<>();
	private final List<RegistroNotificacion> notificaciones = new ArrayList<>();
	private final List<Long> admins = new ArrayList<>();

	@BeforeEach
	void setUp() {
		pedidoRepository = new FakePedidoRepository();
		stockGateway = new FakeStockGateway();
		remitosGenerados.clear();
		notificaciones.clear();
		admins.clear();
		clienteInactivo = false;
		admins.add(99L);
		pedidoService = new PedidoService(pedidoRepository, stockGateway, new ClienteGateway() {
			@Override
			public boolean existeCliente(Long clienteId) {
				return true;
			}

			@Override
			public boolean clienteActivo(Long clienteId) {
				return !clienteInactivo;
			}

			@Override
			public Optional<Long> zonaDeCliente(Long clienteId) {
				return Optional.empty();
			}
		}, id -> true, (pedidoId, clienteId, lineas) -> {
			remitosGenerados.add(pedidoId);
			return 1L;
		}, (tipo, mensaje, paraUsuarioId, pedidoId) -> notificaciones
				.add(new RegistroNotificacion(tipo, mensaje, paraUsuarioId, pedidoId)), new ConsultarUsuario() {
			@Override
			public Optional<Usuario> buscarPorId(Long id) {
				return Optional.empty();
			}

			@Override
			public Optional<Usuario> buscarPorEmail(String email) {
				return Optional.empty();
			}

			@Override
			public List<Usuario> listarTodos() {
				List<Usuario> usuarios = new ArrayList<>();
				for (Long adminId : admins) {
					Usuario admin = new Usuario("Admin", "admin@test.com", "hash",
							new HashSet<>(List.of(Rol.ADMINISTRATIVO)));
					admin.setId(adminId);
					usuarios.add(admin);
				}
				return usuarios;
			}

			@Override
			public com.sistema.common.model.PageResponse<Usuario> listarPaginado(String q, int page, int size) {
				return null;
			}
		}, 48L);
	}

	private Pedido crearPedido(Long itemId, BigDecimal cantidad) {
		return pedidoService.crearPedido(new CrearPedidoCommand(1L, 1L, null, null, false,
				List.of(new LineaPedidoCommand(itemId, cantidad, new BigDecimal("5.00")))));
	}

	@Test
	void contadoresDevuelveTodosLosEstados() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		Map<EstadoPedido, Long> contadores = pedidoService.contadores();

		assertEquals(EstadoPedido.values().length, contadores.size());
		assertEquals(1L, contadores.get(EstadoPedido.PENDIENTE_PREPARACION));
	}

	@Test
	void listarPaginadoDivideEnPaginas() {
		for (int i = 0; i < 3; i++) {
			crearPedido(10L + i, new BigDecimal("1.000"));
		}
		com.sistema.common.model.PageResponse<Pedido> pagina0 = pedidoService.listarPaginado(null, null, null, 0, 2);
		com.sistema.common.model.PageResponse<Pedido> pagina1 = pedidoService.listarPaginado(null, null, null, 1, 2);

		assertEquals(2, pagina0.content().size());
		assertEquals(1, pagina1.content().size());
		assertEquals(3, pagina0.totalElements());
		assertEquals(2, pagina0.totalPages());
	}

	@Test
	void listarPaginadoPorEstadoYFechaFiltraPorAmbos() {
		Pedido enViajeHoy = crearPedido(10L, new BigDecimal("1.000"));
		enViajeHoy.setEstado(EstadoPedido.EN_VIAJE);
		enViajeHoy.setFechaJornada(LocalDate.of(2026, 8, 16));
		pedidoRepository.save(enViajeHoy);

		Pedido enViajeOtroDia = crearPedido(11L, new BigDecimal("1.000"));
		enViajeOtroDia.setEstado(EstadoPedido.EN_VIAJE);
		enViajeOtroDia.setFechaJornada(LocalDate.of(2026, 8, 15));
		pedidoRepository.save(enViajeOtroDia);

		Pedido otroEstadoHoy = crearPedido(12L, new BigDecimal("1.000"));
		otroEstadoHoy.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		otroEstadoHoy.setFechaJornada(LocalDate.of(2026, 8, 16));
		pedidoRepository.save(otroEstadoHoy);

		com.sistema.common.model.PageResponse<Pedido> pagina = pedidoService
				.listarPaginadoPorEstadoYFecha(EstadoPedido.EN_VIAJE, LocalDate.of(2026, 8, 16), 0, 20);

		assertEquals(1, pagina.totalElements());
		assertEquals(enViajeHoy.getId(), pagina.content().get(0).getId());
	}

	@Test
	void listarPorIdsDevuelveSoloLosPedidosSolicitados() {
		Pedido a = crearPedido(10L, new BigDecimal("1.000"));
		Pedido b = crearPedido(11L, new BigDecimal("1.000"));
		Pedido c = crearPedido(12L, new BigDecimal("1.000"));

		List<Pedido> resultado = pedidoService.listarPorIds(List.of(a.getId(), c.getId()));

		assertEquals(2, resultado.size());
		assertTrue(resultado.stream().anyMatch(p -> p.getId().equals(a.getId())));
		assertTrue(resultado.stream().anyMatch(p -> p.getId().equals(c.getId())));
		assertFalse(resultado.stream().anyMatch(p -> p.getId().equals(b.getId())));
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
	void crearPedidoExpressPersisteFlag() {
		Pedido pedido = pedidoService.crearPedido(new CrearPedidoCommand(1L, 1L, null, null, true,
				List.of(new LineaPedidoCommand(10L, new BigDecimal("1"), new BigDecimal("5.00")))));

		assertTrue(pedido.isExpress());
	}

	@Test
	void crearPedidoConItemInactivoLanzaBusinessException() {
		stockGateway.itemsInactivos.add(10L);

		assertThrows(BusinessException.class, () -> crearPedido(10L, new BigDecimal("10.000")));
	}

	@Test
	void crearPedidoConClienteInactivoLanzaBusinessException() {
		clienteInactivo = true;

		assertThrows(BusinessException.class, () -> crearPedido(10L, new BigDecimal("10.000")));
	}

	@Test
	void listarColaPreparacionOrdenaExpressPrimeroYLuegoPorFecha() {
		Pedido normal = crearPedido(10L, new BigDecimal("1.000"));
		normal.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		pedidoRepository.save(normal);

		Pedido express = pedidoService.crearPedido(new CrearPedidoCommand(1L, 1L, null, null, true,
				List.of(new LineaPedidoCommand(11L, new BigDecimal("1"), new BigDecimal("5.00")))));
		express.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		pedidoRepository.save(express);

		com.sistema.common.model.PageResponse<Pedido> cola = pedidoService.listarPaginado(EstadoPedido.PENDIENTE_PREPARACION, null, null, 0, 20);

		assertEquals(2, cola.content().size());
		assertTrue(cola.content().get(0).isExpress(), "El express debe aparecer primero en la cola de preparación");
		assertTrue(cola.content().get(1).isExpress() == false, "El pedido normal debe quedar después del express");
	}

	@Test
	void listarColaConfirmacionOrdenaDeterminista() {
		Pedido p1 = crearPedido(10L, new BigDecimal("1.000"));
		Pedido p2 = crearPedido(10L, new BigDecimal("1.000"));
		Pedido p3 = crearPedido(10L, new BigDecimal("1.000"));
		LocalDateTime mismoTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0);
		for (Pedido p : List.of(p1, p2, p3)) {
			p.setEstado(EstadoPedido.PENDIENTE_CONFIRMACION);
			p.setFechaCreacion(mismoTimestamp);
			pedidoRepository.save(p);
		}
		Pedido express = pedidoService.crearPedido(new CrearPedidoCommand(1L, 1L, null, null, true,
				List.of(new LineaPedidoCommand(11L, new BigDecimal("1"), new BigDecimal("5.00")))));
		express.setEstado(EstadoPedido.PENDIENTE_CONFIRMACION);
		express.setFechaCreacion(mismoTimestamp);
		pedidoRepository.save(express);

		com.sistema.common.model.PageResponse<Pedido> cola = pedidoService.listarPaginado(
				EstadoPedido.PENDIENTE_CONFIRMACION, null, null, 0, 20);

		assertEquals(4, cola.content().size());
		assertTrue(cola.content().get(0).isExpress(), "El express debe aparecer primero en la cola de confirmación");
		List<Long> ids = cola.content().stream().map(Pedido::getId).toList();
		assertEquals(List.of(express.getId(), p1.getId(), p2.getId(), p3.getId()), ids,
				"Con mismo timestamp y mismo express el desempate es por id ASC");
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

		assertEquals(EstadoPedido.PENDIENTE_STOCK, confirmado.getEstado());
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
		assertTrue(remitosGenerados.contains(pedido.getId()));
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
		assertTrue(remitosGenerados.contains(pedido.getId()));
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

		assertEquals(EstadoPedido.PENDIENTE_PREPARACION, modificado.getEstado());
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

	@Test
	void despacharPasaDePreparacionAEntrega() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		Pedido despachado = pedidoService.despachar(pedido.getId());

		assertEquals(EstadoPedido.PENDIENTE_ENTREGA, despachado.getEstado());
	}

	@Test
	void despacharDesdeOtroEstadoLanzaBusinessException() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		assertThrows(BusinessException.class, () -> pedidoService.despachar(pedido.getId()));
	}

	@Test
	void reAgendarDesdeEnViaje() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);

		Pedido reagendado = pedidoService.reAgendar(pedido.getId());

		assertEquals(EstadoPedido.RE_AGENDADO, reagendado.getEstado());
		assertTrue(stockGateway.operaciones.stream().noneMatch(o -> o.startsWith("LIBERACION:")));
	}

	@Test
	void rechazarDesdeEnViajeLiberaTodaLaReserva() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		pedidoRepository.save(pedido);

		pedidoService.rechazarPedido(pedido.getId());

		Pedido recargado = pedidoRepository.findById(pedido.getId()).orElseThrow();
		assertEquals(EstadoPedido.RECHAZADO, recargado.getEstado());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("LIBERACION:")));
		assertEquals(0, new BigDecimal("100.000").compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void asignarARutaYaNoAceptaPreparacion() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		assertThrows(BusinessException.class, () -> pedidoService.asignarARuta(pedido.getId()));
	}

	@Test
	void asignarARutaDesdeReAgendadoVuelveAEntrega() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setEstado(EstadoPedido.PENDIENTE_ENTREGA);
		pedidoRepository.save(pedido);
		pedidoService.reAgendar(pedido.getId());

		Pedido asignado = pedidoService.asignarARuta(pedido.getId());

		assertEquals(EstadoPedido.PENDIENTE_ENTREGA, asignado.getEstado());
	}

	@Test
	void ttlCancelaReservasInactivasYLiberaStock() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		pedido.setUpdatedAt(LocalDateTime.now().minusHours(72));
		pedidoRepository.save(pedido);

		int expirados = pedidoService.expirarReservasInactivas();

		assertEquals(1, expirados);
		Pedido recargado = pedidoRepository.findById(pedido.getId()).orElseThrow();
		assertEquals(EstadoPedido.RECHAZADO, recargado.getEstado());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("LIBERACION:")));
		assertEquals(0, new BigDecimal("100.000").compareTo(stockGateway.disponible.get(10L)));
	}

	@Test
	void ttlNoCancelaPedidosRecientes() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		int expirados = pedidoService.expirarReservasInactivas();

		assertEquals(0, expirados);
		assertEquals(EstadoPedido.PENDIENTE_PREPARACION,
				pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado());
	}

	@Test
	void marcarFaltantePasaABackorderRegistraMermaYNotifica() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		stockGateway.lotesDisponibles.put(10L, List.of(5L));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());
		Usuario actor = new Usuario("Depo", "depo@test.com", "hash",
				new HashSet<>(List.of(Rol.ENCARGADO_DEPOSITO)));
		actor.setId(2L);

		Pedido resultante = pedidoService.marcarFaltante(
				new ModificarStockPedido.MarcarFaltanteCommand(pedido.getId(), 10L, new BigDecimal("3.000"),
						"Lote dañado", actor));

		assertEquals(EstadoPedido.PENDIENTE_STOCK, resultante.getEstado());
		assertEquals(0, new BigDecimal("7.000").compareTo(resultante.getItems().get(0).getCantidadReservada()));
		assertTrue(resultante.getItems().get(0).isPendienteStock());
		assertTrue(stockGateway.operaciones.stream().anyMatch(o -> o.startsWith("MERMA:10:5:")));
		assertEquals(1, notificaciones.size());
		assertEquals("FALTANTE_PRODUCTO", notificaciones.get(0).tipo());
		assertEquals(99L, notificaciones.get(0).paraUsuarioId());
		assertEquals(pedido.getId(), notificaciones.get(0).pedidoId());
	}

	@Test
	void marcarFaltanteSoloPermitePreparacion() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		stockGateway.lotesDisponibles.put(10L, List.of(5L));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));

		assertThrows(BusinessException.class, () -> pedidoService.marcarFaltante(
				new ModificarStockPedido.MarcarFaltanteCommand(pedido.getId(), 10L, new BigDecimal("3.000"),
						"Faltante", null)));
	}

	@Test
	void marcarFaltanteSinStockFisicoLanza() {
		stockGateway.disponible.put(10L, new BigDecimal("100.000"));
		Pedido pedido = crearPedido(10L, new BigDecimal("10.000"));
		pedidoService.confirmarPedido(pedido.getId());

		assertThrows(BusinessException.class, () -> pedidoService.marcarFaltante(
				new ModificarStockPedido.MarcarFaltanteCommand(pedido.getId(), 10L, new BigDecimal("3.000"),
						"Faltante", null)));
	}

	@Test
	void consolidarPedidosFusionaLineasYCancelaOrigenes() {
		Pedido p1 = crearPedido(10L, new BigDecimal("2.000"));
		Pedido p2 = crearPedido(10L, new BigDecimal("3.000"));
		Pedido p3 = crearPedido(11L, new BigDecimal("4.000"));

		Pedido consolidado = pedidoService.consolidarPedidos(
				new ModificarStockPedido.ConsolidarCommand(List.of(p1.getId(), p2.getId(), p3.getId()), 1L));

		assertEquals(2, consolidado.getItems().size());
		PedidoItem item10 = consolidado.itemPorItem(10L).orElseThrow();
		assertEquals(0, new BigDecimal("5.000").compareTo(item10.getCantidadPedida()));
		PedidoItem item11 = consolidado.itemPorItem(11L).orElseThrow();
		assertEquals(0, new BigDecimal("4.000").compareTo(item11.getCantidadPedida()));
		assertEquals(EstadoPedido.PENDIENTE_CONFIRMACION, consolidado.getEstado());
		assertEquals(EstadoPedido.RECHAZADO, pedidoRepository.findById(p1.getId()).orElseThrow().getEstado());
		assertEquals(EstadoPedido.RECHAZADO, pedidoRepository.findById(p2.getId()).orElseThrow().getEstado());
	}

	@Test
	void consolidarExigeMismoClienteYPendienteConfirmacion() {
		Pedido p1 = crearPedido(10L, new BigDecimal("2.000"));
		Pedido p2 = crearPedido(10L, new BigDecimal("3.000"));
		p2.setClienteId(2L);
		pedidoRepository.save(p2);
		assertThrows(BusinessException.class, () -> pedidoService.consolidarPedidos(
				new ModificarStockPedido.ConsolidarCommand(List.of(p1.getId(), p2.getId()), 1L)));

		Pedido p3 = crearPedido(10L, new BigDecimal("2.000"));
		p3.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		pedidoRepository.save(p3);
		Pedido p4 = crearPedido(10L, new BigDecimal("1.000"));
		assertThrows(BusinessException.class, () -> pedidoService.consolidarPedidos(
				new ModificarStockPedido.ConsolidarCommand(List.of(p3.getId(), p4.getId()), 1L)));
	}

	private static class FakePedidoRepository implements PedidoRepository {

		private final Map<Long, Pedido> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Pedido save(Pedido pedido) {
			if (pedido.getId() == null) {
				pedido.setId(secuencia.getAndIncrement());
			}
			if (pedido.getUpdatedAt() == null) {
				pedido.setUpdatedAt(LocalDateTime.now());
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
		public long contarPorEstado(EstadoPedido estado) {
			return datos.values().stream().filter(p -> p.getEstado() == estado).count();
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

		@Override
		public List<Pedido> findByEstadoAndFechaJornada(EstadoPedido estado, LocalDate fechaJornada) {
			return datos.values().stream()
					.filter(p -> p.getEstado() == estado && fechaJornada.equals(p.getFechaJornada())).toList();
		}

		@Override
		public List<Pedido> findByIds(Collection<Long> ids) {
			return datos.values().stream().filter(p -> p.getId() != null && ids.contains(p.getId())).toList();
		}
	}

	private static class FakeStockGateway implements StockGateway {

		private final Map<Long, BigDecimal> disponible = new HashMap<>();
		private final List<String> operaciones = new ArrayList<>();
		private final Map<Long, List<Long>> lotesDisponibles = new HashMap<>();
		private final Set<Long> itemsInactivos = new HashSet<>();

		@Override
		public boolean existeItem(Long itemId) {
			return true;
		}

		@Override
		public boolean itemActivo(Long itemId) {
			return !itemsInactivos.contains(itemId);
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

		@Override
		public List<Long> listarLoteIdsDisponibles(Long itemId) {
			return lotesDisponibles.getOrDefault(itemId, List.of());
		}

		@Override
		public void registrarMerma(Long itemId, Long loteId, BigDecimal cantidad, String motivo) {
			operaciones.add("MERMA:" + itemId + ":" + loteId + ":" + cantidad);
		}

		@Override
		public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo) {
			disponible.put(itemId, consultarDisponible(itemId).add(cantidad));
			operaciones.add("INGRESO:" + itemId + ":" + cantidad);
		}

		@Override
		public BigDecimal consultarPrecioLista(Long itemId) {
			return BigDecimal.ZERO;
		}
	}

	private record RegistroNotificacion(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId) {
	}
}
