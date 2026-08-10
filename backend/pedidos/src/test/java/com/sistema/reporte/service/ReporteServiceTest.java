package com.sistema.reporte.service;

import com.sistema.common.model.PageResponse;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.model.PedidoItem;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.reporte.port.in.ConsultarReportes;
import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.stock.model.Item;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReporteServiceTest {

	private ReporteService reporteService;
	private FakeStock fakeStock;
	private FakePedido fakePedido;
	private FakeRuta fakeRuta;

	@BeforeEach
	void setUp() {
		fakeStock = new FakeStock();
		fakePedido = new FakePedido();
		fakeRuta = new FakeRuta();
		ConsultarUsuario fakeUsuario = new ConsultarUsuario() {
			@Override
			public Optional<Usuario> buscarPorId(Long id) {
				Usuario u = new Usuario("Vendedor " + id, "v" + id + "@test.com", "x", java.util.Set.of());
				u.setId(id);
				return Optional.of(u);
			}

			@Override
			public Optional<Usuario> buscarPorEmail(String email) {
				return Optional.empty();
			}

			@Override
			public List<Usuario> listarTodos() {
				return List.of();
			}
		};
		reporteService = new ReporteService(fakeStock, fakePedido, fakeUsuario, fakeRuta);
	}

	private Pedido pedidoEntregado(Long vendedorId, BigDecimal entregada, BigDecimal precio, LocalDateTime fecha) {
		Pedido p = new Pedido(1L, vendedorId, null, null, new ArrayList<>());
		p.setEstado(EstadoPedido.ENTREGADO);
		p.setFechaCreacion(fecha);
		PedidoItem item = new PedidoItem(1L, entregada, precio);
		item.setCantidadReservada(entregada);
		item.setCantidadEntregada(entregada);
		p.agregarItem(item);
		return p;
	}

	@Test
	void stockGeneralReportaDisponibleYReservas() {
		Item i1 = new Item("A", "Item A", "UN");
		i1.setId(1L);
		Item i2 = new Item("B", "Item B", "UN");
		i2.setId(2L);
		fakeStock.items.add(i1);
		fakeStock.items.add(i2);
		fakeStock.disponible.put(1L, new BigDecimal("10.000"));
		fakeStock.reservas.put(1L, new BigDecimal("2.000"));
		fakeStock.disponible.put(2L, new BigDecimal("5.000"));
		fakeStock.reservas.put(2L, BigDecimal.ZERO);

		List<ConsultarReportes.ItemStockReporte> reporte = reporteService.stockGeneral();

		assertEquals(2, reporte.size());
		assertEquals(0, new BigDecimal("10.000").compareTo(reporte.get(0).disponible()));
		assertEquals(0, new BigDecimal("2.000").compareTo(reporte.get(0).reservasActivas()));
	}

	@Test
	void ventasSoloCuentaEntregadosYSumaPorVendedor() {
		fakePedido.pedidos.add(pedidoEntregado(1L, new BigDecimal("10.000"), new BigDecimal("5.00"), LocalDateTime.of(2026, 8, 1, 10, 0)));
		fakePedido.pedidos.add(pedidoEntregado(1L, new BigDecimal("8.000"), new BigDecimal("5.00"), LocalDateTime.of(2026, 8, 2, 10, 0)));
		Pedido pendiente = pedidoEntregado(1L, new BigDecimal("99.000"), new BigDecimal("5.00"), LocalDateTime.of(2026, 8, 3, 10, 0));
		pendiente.setEstado(EstadoPedido.PENDIENTE_CONFIRMACION);
		fakePedido.pedidos.add(pendiente);

		List<ConsultarReportes.VentaVendedorReporte> ventas = reporteService.ventasPorVendedor(null, null, null);

		assertEquals(1, ventas.size());
		assertEquals(2, ventas.get(0).cantidadPedidos());
		assertEquals(0, new BigDecimal("18.000").compareTo(ventas.get(0).cantidadUnidades()));
		assertEquals(0, new BigDecimal("90.00").compareTo(ventas.get(0).monto()));
	}

	@Test
	void ventasFiltraPorVendedorYFechas() {
		fakePedido.pedidos.add(pedidoEntregado(1L, new BigDecimal("10.000"), new BigDecimal("5.00"), LocalDateTime.of(2026, 8, 1, 10, 0)));
		fakePedido.pedidos.add(pedidoEntregado(2L, new BigDecimal("20.000"), new BigDecimal("5.00"), LocalDateTime.of(2026, 8, 1, 10, 0)));

		List<ConsultarReportes.VentaVendedorReporte> ventas = reporteService.ventasPorVendedor(1L,
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

		assertEquals(1, ventas.size());
		assertEquals(1L, ventas.get(0).vendedorId());
	}

	@Test
	void rutasPorFechaCuentaPedidos() {
		Ruta r1 = new Ruta(1L, 10L, LocalDate.of(2026, 8, 10));
		r1.setId(1L);
		r1.setEstado(EstadoRuta.EN_CURSO);
		r1.asignarPedidos(List.of(100L, 101L, 102L));
		Ruta r2 = new Ruta(1L, 10L, LocalDate.of(2026, 8, 11));
		r2.setId(2L);
		r2.asignarPedidos(List.of(200L));
		fakeRuta.rutas.add(r1);
		fakeRuta.rutas.add(r2);

		List<ConsultarReportes.RutaReporte> reporte = reporteService.rutasPorFecha(LocalDate.of(2026, 8, 10));

		assertEquals(1, reporte.size());
		assertEquals(3, reporte.get(0).cantidadPedidos());
		assertEquals("EN_CURSO", reporte.get(0).estado());
	}

	private static class FakeStock implements ConsultarStock {
		private final List<Item> items = new ArrayList<>();
		private final Map<Long, BigDecimal> disponible = new HashMap<>();
		private final Map<Long, BigDecimal> reservas = new HashMap<>();

		@Override
		public Optional<Item> buscarItemPorId(Long id) {
			return items.stream().filter(i -> i.getId().equals(id)).findFirst();
		}

		@Override
		public List<Item> listarItems() {
			return items;
		}

		@Override
		public BigDecimal obtenerDisponible(Long itemId) {
			return disponible.getOrDefault(itemId, BigDecimal.ZERO);
		}

		@Override
		public BigDecimal obtenerReservasActivas(Long itemId) {
			return reservas.getOrDefault(itemId, BigDecimal.ZERO);
		}

		@Override
		public List<com.sistema.stock.model.MovimientoStock> listarMovimientos(Long itemId) {
			return List.of();
		}

		@Override
		public List<com.sistema.stock.model.Lote> listarLotes(Long itemId) {
			return List.of();
		}

		@Override
		public List<com.sistema.stock.model.Lote> listarLotesPorVencer(int dias) {
			return List.of();
		}
	}

	private static class FakePedido implements ConsultarPedido {
		private final List<Pedido> pedidos = new ArrayList<>();

		@Override
		public Optional<Pedido> buscarPorId(Long id) {
			return pedidos.stream().filter(p -> p.getId() != null && p.getId().equals(id)).findFirst();
		}

		@Override
		public List<Pedido> listarTodos() {
			return pedidos;
		}

		@Override
		public List<Pedido> listarPorEstado(EstadoPedido estado) {
			return pedidos.stream().filter(p -> p.getEstado() == estado).toList();
		}

		@Override
		public List<Pedido> listarPorCliente(Long clienteId) {
			return pedidos.stream().filter(p -> p.getClienteId().equals(clienteId)).toList();
		}

		@Override
		public List<Pedido> listarPorVendedor(Long vendedorId) {
			return pedidos.stream().filter(p -> p.getVendedorId().equals(vendedorId)).toList();
		}

		@Override
		public List<Pedido> listarHijosDe(Long pedidoPadreId) {
			return pedidos.stream().filter(p -> pedidoPadreId.equals(p.getPedidoPadreId())).toList();
		}

		@Override
		public PageResponse<Pedido> listarPaginado(EstadoPedido estado, Long clienteId, Long vendedorId,
				int page, int size) {
			List<Pedido> todos;
			if (estado != null) {
				todos = listarPorEstado(estado);
			} else if (clienteId != null) {
				todos = listarPorCliente(clienteId);
			} else if (vendedorId != null) {
				todos = listarPorVendedor(vendedorId);
			} else {
				todos = listarTodos();
			}
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}
	}

	private static class FakeRuta implements ConsultarRuta {
		private final List<Ruta> rutas = new ArrayList<>();

		@Override
		public Optional<Ruta> buscarPorId(Long id) {
			return rutas.stream().filter(r -> r.getId().equals(id)).findFirst();
		}

		@Override
		public List<Ruta> listarTodos() {
			return rutas;
		}

		@Override
		public List<Ruta> listarPorFecha(LocalDate fechaJornada) {
			return rutas.stream().filter(r -> r.getFechaJornada().equals(fechaJornada)).toList();
		}

		@Override
		public List<Ruta> listarPorRepartidor(Long repartidorId) {
			return rutas.stream().filter(r -> r.getRepartidorId().equals(repartidorId)).toList();
		}

		@Override
		public List<Ruta> listarPorEstado(EstadoRuta estado) {
			return rutas.stream().filter(r -> r.getEstado() == estado).toList();
		}
	}
}
