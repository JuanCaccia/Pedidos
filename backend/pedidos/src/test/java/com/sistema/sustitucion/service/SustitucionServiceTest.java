package com.sistema.sustitucion.service;

import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.model.FormaPago;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.model.PageResponse;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.sustitucion.model.Sustitucion;
import com.sistema.sustitucion.port.in.RegistrarSustitucion.SustituirCommand;
import com.sistema.stock.port.in.AjustarInventario;
import com.sistema.sustitucion.port.out.StockGateway;
import com.sistema.sustitucion.port.out.SustitucionRepository;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SustitucionServiceTest {

	private SustitucionService sustitucionService;
	private FakeSustitucionRepository sustitucionRepository;
	private FakeStockGateway stockGateway;
	private FakeRegistrarCobranza registrarCobranza;
	private FakeAjustarInventario ajustarInventario;
	private final Usuario actor = actor();

	private Usuario actor() {
		Usuario u = new Usuario("Repartidor", "rep@test.com", "x", Set.of(Rol.REPARTIDOR));
		u.setId(1L);
		return u;
	}

	@BeforeEach
	void setUp() {
		sustitucionRepository = new FakeSustitucionRepository();
		stockGateway = new FakeStockGateway();
		registrarCobranza = new FakeRegistrarCobranza();
		ajustarInventario = new FakeAjustarInventario();
		sustitucionService = new SustitucionService(sustitucionRepository, stockGateway,
				new FakeConsultarPedido(EstadoPedido.ENTREGADO), registrarCobranza, ajustarInventario);
	}

	@Test
	void sustituirRegistraIngresoEgresoYBalance() {
		stockGateway.precio.put(1L, new BigDecimal("3.00"));
		stockGateway.precio.put(2L, new BigDecimal("5.00"));

		Sustitucion sustitucion = sustitucionService.sustituir(
				new SustituirCommand(10L, 1L, 2L, new BigDecimal("2"), null, actor));

		assertNotNull(sustitucion.getId());
		assertEquals(0, new BigDecimal("-4.00").compareTo(sustitucion.getDiferenciaPrecio()));

		assertEquals(1, stockGateway.ingresos.size());
		assertEquals(1L, stockGateway.ingresos.get(0).itemId);
		assertEquals(new BigDecimal("2"), stockGateway.ingresos.get(0).cantidad);

		// El sustituto sale sin reserva: se descuenta con un ajuste negativo (no un EGRESO,
		// porque la fórmula de disponible cancela los egresos).
		assertEquals(1, ajustarInventario.ajustes.size());
		assertEquals(2L, ajustarInventario.ajustes.get(0).itemId);
		assertEquals(0, new BigDecimal("-2").compareTo(ajustarInventario.ajustes.get(0).cantidad));

		assertEquals(1, registrarCobranza.registrados.size());
		RegistrarCobranza.RegistrarCobranzaCommand cobranza = registrarCobranza.registrados.get(0);
		assertEquals(7L, cobranza.clienteId());
		assertEquals(0, new BigDecimal("-4.00").compareTo(cobranza.monto()));
		assertEquals(FormaPago.OTRO, cobranza.formaPago());
	}

	@Test
	void sustituirSoloEnEntregadoOParcial() {
		SustitucionService service = new SustitucionService(sustitucionRepository, stockGateway,
				new FakeConsultarPedido(EstadoPedido.PENDIENTE_PREPARACION), registrarCobranza, ajustarInventario);

		assertThrows(BusinessException.class, () -> service.sustituir(
				new SustituirCommand(10L, 1L, 2L, new BigDecimal("1"), null, actor)));
	}

	@Test
	void sustituirMismoItemLanza() {
		assertThrows(BusinessException.class, () -> sustitucionService.sustituir(
				new SustituirCommand(10L, 1L, 1L, new BigDecimal("1"), null, actor)));
	}

	private record RegistroIngreso(Long itemId, BigDecimal cantidad, String motivo) {
	}

	private record RegistroAjuste(Long itemId, BigDecimal cantidad, String motivo) {
	}

	private static class FakeConsultarPedido implements ConsultarPedido {

		private final EstadoPedido estado;

		FakeConsultarPedido(EstadoPedido estado) {
			this.estado = estado;
		}

		@Override
		public Optional<Pedido> buscarPorId(Long id) {
			Pedido pedido = new Pedido();
			pedido.setId(id);
			pedido.setClienteId(7L);
			pedido.setEstado(estado);
			return Optional.of(pedido);
		}

		@Override
		public List<Pedido> listarTodos() {
			return List.of();
		}

		@Override
		public List<Pedido> listarPorEstado(EstadoPedido estado) {
			return List.of();
		}

		@Override
		public List<Pedido> listarPorCliente(Long clienteId) {
			return List.of();
		}

		@Override
		public List<Pedido> listarPorVendedor(Long vendedorId) {
			return List.of();
		}

		@Override
		public List<Pedido> listarHijosDe(Long pedidoPadreId) {
			return List.of();
		}

		@Override
		public Map<EstadoPedido, Long> contadores() {
			return Map.of();
		}

		@Override
		public PageResponse<Pedido> listarPaginado(EstadoPedido estado, Long clienteId, Long vendedorId, int page,
				int size) {
			return new PageResponse<>(List.of(), page, size, 0, 0);
		}
	}

	private static class FakeSustitucionRepository implements SustitucionRepository {

		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Sustitucion save(Sustitucion s) {
			if (s.getId() == null) {
				s.setId(secuencia.getAndIncrement());
			}
			return s;
		}
	}

	private static class FakeStockGateway implements StockGateway {

		final Map<Long, BigDecimal> precio = new java.util.HashMap<>();
		final List<RegistroIngreso> ingresos = new ArrayList<>();

		@Override
		public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo) {
			ingresos.add(new RegistroIngreso(itemId, cantidad, motivo));
		}

		@Override
		public BigDecimal consultarPrecioLista(Long itemId) {
			return precio.getOrDefault(itemId, BigDecimal.ZERO);
		}
	}

	private static class FakeAjustarInventario implements AjustarInventario {

		final List<RegistroAjuste> ajustes = new ArrayList<>();

		@Override
		public com.sistema.stock.model.MovimientoStock ajustarInventario(AjusteInventarioCommand command) {
			ajustes.add(new RegistroAjuste(command.itemId(), command.cantidad(), command.motivo()));
			return null;
		}
	}

	private static class FakeRegistrarCobranza implements RegistrarCobranza {

		final List<RegistrarCobranzaCommand> registrados = new ArrayList<>();

		@Override
		public Cobranza registrar(RegistrarCobranzaCommand command) {
			registrados.add(command);
			Cobranza c = new Cobranza(command.clienteId(), command.pedidoId(), command.monto(), command.formaPago(),
					LocalDateTime.now(), command.observaciones());
			c.setId(1L);
			return c;
		}
	}
}
