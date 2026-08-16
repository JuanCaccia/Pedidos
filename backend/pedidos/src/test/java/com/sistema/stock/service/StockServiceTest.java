package com.sistema.stock.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.stock.adapter.in.web.dto.LoteResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.model.TipoMovimiento;
import com.sistema.stock.port.in.AjustarInventario;
import com.sistema.stock.port.in.GestionarItem;
import com.sistema.stock.port.in.GestionarMerma;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.port.out.ItemRepository;
import com.sistema.stock.port.out.LoteRepository;
import com.sistema.stock.port.out.MovimientoStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

class StockServiceTest {

	private StockService stockService;
	private FakeItemRepository itemRepository;
	private FakeLoteRepository loteRepository;
	private FakeMovimientoStockRepository movimientoRepository;

	@BeforeEach
	void setUp() {
		itemRepository = new FakeItemRepository();
		loteRepository = new FakeLoteRepository();
		movimientoRepository = new FakeMovimientoStockRepository();
		stockService = new StockService(itemRepository, loteRepository, movimientoRepository, new BigDecimal("50"));
	}

	private com.sistema.usuario.model.Usuario adminActor() {
		com.sistema.usuario.model.Usuario u = new com.sistema.usuario.model.Usuario("Admin", "admin@test.com", "x",
				java.util.Set.of(com.sistema.usuario.model.Rol.ADMINISTRATIVO));
		u.setId(1L);
		return u;
	}

	private com.sistema.usuario.model.Usuario encargadoActor() {
		com.sistema.usuario.model.Usuario u = new com.sistema.usuario.model.Usuario("Enc", "enc@test.com", "x",
				java.util.Set.of(com.sistema.usuario.model.Rol.ENCARGADO_DEPOSITO));
		u.setId(2L);
		return u;
	}

	private Long itemConIngreso(String sku, BigDecimal cantidad) {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand(sku, "Item " + sku, "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), "LOTE-" + sku, null, cantidad, "Test"));
		return item.getId();
	}

	@Test
	void ingresoCreaLoteYMovimiento() {
		Long itemId = itemConIngreso("A", new BigDecimal("100.000"));
		assertEquals(0, new BigDecimal("100.000").compareTo(stockService.obtenerDisponible(itemId)));
		assertEquals(1, loteRepository.findByItemId(itemId).size());
		assertEquals(1, movimientoRepository.findByItemIdOrderByFechaAsc(itemId).size());
	}

	@Test
	void formulaReservaYEntregaParcial() {
		Long itemId = itemConIngreso("B", new BigDecimal("100.000"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.RESERVA_PEDIDO, itemId, null, 1L,
				new BigDecimal("10.000"), LocalDateTime.now(), "reserva pedido 1"));
		// Entrega parcial: 8 salen como EGRESO (cierran reserva) y 2 se liberan
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.EGRESO_VENTA, itemId, null, 1L,
				new BigDecimal("8.000"), LocalDateTime.now(), "entrega parcial"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.LIBERACION_RESERVA, itemId, null, 1L,
				new BigDecimal("2.000"), LocalDateTime.now(), "libera sobrante"));

		assertEquals(0, new BigDecimal("92.000").compareTo(stockService.obtenerDisponible(itemId)));
		assertEquals(0, new BigDecimal("0.000").compareTo(stockService.obtenerReservasActivas(itemId)));
	}

	@Test
	void formulaReservaActivaSinEntrega() {
		Long itemId = itemConIngreso("C", new BigDecimal("100.000"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.RESERVA_PEDIDO, itemId, null, 1L,
				new BigDecimal("10.000"), LocalDateTime.now(), "reserva"));

		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponible(itemId)));
		assertEquals(0, new BigDecimal("10.000").compareTo(stockService.obtenerReservasActivas(itemId)));
	}

	@Test
	void mermaReduceDisponible() {
		Long itemId = itemConIngreso("D", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		stockService.registrarMerma(new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(),
				new BigDecimal("5.000"), "Ruptura de envase"));

		assertEquals(0, new BigDecimal("95.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void mermaSinMotivoOLoteDeOtroItemLanzaBusinessException() {
		Long itemId = itemConIngreso("E", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);

		assertThrows(BusinessException.class, () -> stockService.registrarMerma(
				new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(), new BigDecimal("5.000"), "  ")));

		Long otroItemId = itemConIngreso("F", new BigDecimal("100.000"));
		assertThrows(BusinessException.class, () -> stockService.registrarMerma(
				new GestionarMerma.RegistrarMermaCommand(otroItemId, lote.getId(), new BigDecimal("5.000"), "motivo")));
	}

	@Test
	void mermaMayorAlStockFisicoDelLoteLanza() {
		Long itemId = itemConIngreso("G", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		assertThrows(BusinessException.class, () -> stockService.registrarMerma(
				new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(), new BigDecimal("150.000"), "motivo")));
	}

	@Test
	void mermaDeStockReservadoPermitida() {
		Long itemId = itemConIngreso("H2", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.RESERVA_PEDIDO, itemId, null, 1L,
				new BigDecimal("90.000"), LocalDateTime.now(), "reserva"));

		stockService.registrarMerma(new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(),
				new BigDecimal("50.000"), "lote dañado"));

		assertEquals(0, new BigDecimal("-40.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteNegativoReduceDisponible() {
		Long itemId = itemConIngreso("H", new BigDecimal("100.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "Diferencia fisica", adminActor()));

		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteInvalidoOLlevaANegativoLanzaBusinessException() {
		Long itemId = itemConIngreso("I", new BigDecimal("100.000"));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, BigDecimal.ZERO, "motivo", adminActor())));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-999.000"), "  ", adminActor())));
	}

	@Test
	void ajusteGrandeConEncargadoLanzaBusinessException() {
		Long itemId = itemConIngreso("I2", new BigDecimal("1000.000"));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-200.000"), "grande", encargadoActor())));
	}

	@Test
	void ajusteGrandeConAdminOk() {
		Long itemId = itemConIngreso("I3", new BigDecimal("1000.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-200.000"), "grande", adminActor()));
		assertEquals(0, new BigDecimal("800.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteChicoConEncargadoOk() {
		Long itemId = itemConIngreso("I4", new BigDecimal("1000.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "chico", encargadoActor()));
		assertEquals(0, new BigDecimal("990.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void egresoConsumeLoteFefo() {
		LocalDate hoy = LocalDate.now();
		loteRepository.save(new Lote(1L, "L-VENCE-LEJOS", hoy, hoy.plusDays(30), new BigDecimal("100.000")));
		loteRepository.save(new Lote(1L, "L-VENCE-CERCA", hoy, hoy.plusDays(10), new BigDecimal("100.000")));
		loteRepository.save(new Lote(1L, "L-SIN-VENC", hoy, null, new BigDecimal("100.000")));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, 1L, 1L, null, new BigDecimal("100.000"), hoy.atStartOfDay(), "i1"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, 1L, 2L, null, new BigDecimal("100.000"), hoy.atStartOfDay(), "i2"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, 1L, 3L, null, new BigDecimal("100.000"), hoy.atStartOfDay(), "i3"));

		stockService.egresarPorLotes(1L, 99L, new BigDecimal("150.000"));

		Map<Long, BigDecimal> porLote = new HashMap<>();
		for (MovimientoStock m : movimientoRepository.findByItemIdOrderByFechaAsc(1L)) {
			if (m.getTipo() == TipoMovimiento.EGRESO_VENTA && m.getLoteId() != null) {
				porLote.merge(m.getLoteId(), m.getCantidad(), BigDecimal::add);
			}
		}
		assertEquals(0, new BigDecimal("100.000").compareTo(porLote.getOrDefault(2L, BigDecimal.ZERO))); // vence cerca -> 100
		assertEquals(0, new BigDecimal("50.000").compareTo(porLote.getOrDefault(1L, BigDecimal.ZERO)));  // vence lejos -> 50
		assertEquals(0, porLote.getOrDefault(3L, BigDecimal.ZERO).compareTo(BigDecimal.ZERO));           // sin vencimiento -> 0
	}

	@Test
	void egresoFefoNoConsumeLoteVencido() {
		LocalDate hoy = LocalDate.now();
		// Lote vencido (id 1) y lote válido (id 2), ambos con stock.
		loteRepository.save(new Lote(1L, "L-VENCIDO", hoy.minusDays(60), hoy.minusDays(2), new BigDecimal("100.000")));
		loteRepository.save(new Lote(1L, "L-VALIDO", hoy, hoy.plusDays(30), new BigDecimal("100.000")));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, 1L, 1L, null, new BigDecimal("100.000"), hoy.atStartOfDay(), "i-vencido"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, 1L, 2L, null, new BigDecimal("100.000"), hoy.atStartOfDay(), "i-valido"));

		stockService.egresarPorLotes(1L, 99L, new BigDecimal("150.000"));

		Map<Long, BigDecimal> porLote = new HashMap<>();
		for (MovimientoStock m : movimientoRepository.findByItemIdOrderByFechaAsc(1L)) {
			if (m.getTipo() == TipoMovimiento.EGRESO_VENTA && m.getLoteId() != null) {
				porLote.merge(m.getLoteId(), m.getCantidad(), BigDecimal::add);
			}
		}
		assertEquals(0, porLote.getOrDefault(1L, BigDecimal.ZERO).compareTo(BigDecimal.ZERO));           // vencido -> 0
		assertEquals(0, new BigDecimal("100.000").compareTo(porLote.getOrDefault(2L, BigDecimal.ZERO))); // válido -> 100
		// Lo que no cubre el lote válido sale como egreso sin lote.
		assertTrue(movimientoRepository.findByItemIdOrderByFechaAsc(1L).stream()
				.anyMatch(m -> m.getTipo() == TipoMovimiento.EGRESO_VENTA && m.getLoteId() == null));
	}

	@Test
	void mermaSobreItemInactivoLanzaBusinessException() {
		Long itemId = itemConIngreso("MI", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		stockService.desactivarItem(itemId);

		assertThrows(BusinessException.class, () -> stockService.registrarMerma(
				new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(), new BigDecimal("5.000"), "motivo")));
	}

	@Test
	void ajusteSobreItemInactivoLanzaBusinessException() {
		Long itemId = itemConIngreso("AI", new BigDecimal("100.000"));
		stockService.desactivarItem(itemId);

		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-10.000"), "ajuste", adminActor())));
	}

	@Test
	void listarItemsActivosExcluyeInactivos() {
		Item activo = stockService.crearItem(new GestionarItem.CrearItemCommand("ACT-1", "Activo", "UN", null, null, null));
		Item inactivo = stockService.crearItem(new GestionarItem.CrearItemCommand("INACT-1", "Inactivo", "UN", null, null, null));
		stockService.desactivarItem(inactivo.getId());

		PageResponse<Item> soloActivos = stockService.listarItemsActivosPaginado(null, null, 0, 20);
		PageResponse<Item> todos = stockService.listarItemsPaginado(null, null, 0, 20);

		assertEquals(1, soloActivos.content().size());
		assertEquals("ACT-1", soloActivos.content().get(0).getSku());
		assertEquals(2, todos.content().size());
		assertTrue(todos.content().stream().anyMatch(i -> i.getId().equals(inactivo.getId())));
	}

	@Test
	void itemDuplicadoOInexistente() {
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-X", "X", "UN", null, null, null));
		assertThrows(BusinessException.class, () -> stockService.crearItem(
				new GestionarItem.CrearItemCommand("sku-x", "Y", "UN", null, null, null)));
		assertThrows(NotFoundException.class, () -> stockService.desactivarItem(999L));
	}

	@Test
	void desactivarItemPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-Y", "Y", "UN", null, null, null));
		stockService.desactivarItem(item.getId());
		assertFalse(itemRepository.findById(item.getId()).orElseThrow().isActivo());
	}

	@Test
	void reactivarItemPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-RX", "Rx", "UN", null, null, null));
		stockService.desactivarItem(item.getId());
		assertFalse(itemRepository.findById(item.getId()).orElseThrow().isActivo());

		stockService.reactivarItem(item.getId());

		assertTrue(itemRepository.findById(item.getId()).orElseThrow().isActivo());
	}

	@Test
	void actualizarItemCambiaNombreYUnidad() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-MOD", "Original", "UN", null, null, null));

		Item actualizado = stockService.actualizarItem(new GestionarItem.ActualizarItemCommand(
				item.getId(), "Modificado", "KG", null, null, null));

		assertEquals("Modificado", actualizado.getNombre());
		assertEquals("KG", actualizado.getUnidadMedida());
		assertEquals("SKU-MOD", actualizado.getSku());
	}

	@Test
	void actualizarItemConDatosInvalidosOInexistenteLanza() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-MOD2", "Original", "UN", null, null, null));

		assertThrows(BusinessException.class, () -> stockService.actualizarItem(
				new GestionarItem.ActualizarItemCommand(item.getId(), "  ", "UN", null, null, null)));
		assertThrows(NotFoundException.class, () -> stockService.actualizarItem(
				new GestionarItem.ActualizarItemCommand(999L, "X", "UN", null, null, null)));
	}

	@Test
	void crearItemConStockMinimoPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-MIN", "Min", "UN", new BigDecimal("20.000"), null, null));
		assertEquals(0, new BigDecimal("20.000").compareTo(item.getStockMinimo()));
	}

	@Test
	void stockMinimoNegativoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> stockService.crearItem(
				new GestionarItem.CrearItemCommand("SKU-MINN", "Min", "UN", new BigDecimal("-1.000"), null, null)));
	}

	@Test
	void actualizarItemCambiaStockMinimo() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-MIN2", "Min", "UN", null, null, null));
		Item actualizado = stockService.actualizarItem(new GestionarItem.ActualizarItemCommand(
				item.getId(), "Min2", "UN", new BigDecimal("15.000"), null, null));
		assertEquals(0, new BigDecimal("15.000").compareTo(actualizado.getStockMinimo()));
	}

	@Test
	void crearItemConPrecioListaPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand(
				"SKU-PRECIO", "Con Precio", "UN", null, new BigDecimal("12.50"), null));
		assertEquals(0, new BigDecimal("12.50").compareTo(item.getPrecioLista()));
	}

	@Test
	void precioListaNegativoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> stockService.crearItem(
				new GestionarItem.CrearItemCommand("SKU-PRECIO-N", "Negativo", "UN", null, new BigDecimal("-1.00"), null)));
	}

	@Test
	void actualizarItemCambiaPrecioLista() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-PRECIO-2", "Precio", "UN", null, null, null));
		Item actualizado = stockService.actualizarItem(new GestionarItem.ActualizarItemCommand(
				item.getId(), "Precio", "UN", null, new BigDecimal("20.00"), null));
		assertEquals(0, new BigDecimal("20.00").compareTo(actualizado.getPrecioLista()));
	}

	@Test
	void lotesPorVencerDevuelveSoloLosProximos() {
		LocalDate hoy = LocalDate.now();
		loteRepository.save(new Lote(1L, "L-VENCE", hoy, hoy.plusDays(10), new BigDecimal("5.000")));
		loteRepository.save(new Lote(1L, "L-VENCIDO", hoy, hoy.minusDays(3), new BigDecimal("5.000")));
		loteRepository.save(new Lote(1L, "L-LEJOS", hoy, hoy.plusDays(90), new BigDecimal("5.000")));

		List<Lote> resultado = stockService.listarLotesPorVencer(30);

		assertEquals(2, resultado.size());
	}

	@Test
	void disponibleDeLoteRestaEgresosYMermas() {
		Long itemId = itemConIngreso("DISLOTE", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.EGRESO_VENTA, itemId, lote.getId(), 1L,
				new BigDecimal("30.000"), LocalDateTime.now(), "egreso"));
		movimientoRepository.save(new MovimientoStock(TipoMovimiento.MERMA, itemId, lote.getId(), null,
				new BigDecimal("10.000"), LocalDateTime.now(), "merma"));

		assertEquals(0, new BigDecimal("60.000").compareTo(stockService.obtenerDisponibleDeLote(itemId, lote.getId())));
	}

	@Test
	void listarTodosLosLotesDevuelveTodos() {
		LocalDate hoy = LocalDate.now();
		loteRepository.save(new Lote(1L, "L1", hoy, hoy.plusDays(10), new BigDecimal("5.000")));
		loteRepository.save(new Lote(2L, "L2", hoy, null, new BigDecimal("7.000")));

		List<Lote> resultado = stockService.listarTodosLosLotes();

		assertEquals(2, resultado.size());
	}

	@Test
	void estadoDerivadoVencidoAgotadoYVigente() {
		LocalDate hoy = LocalDate.now();
		// Vencido: fecha anterior a hoy, sin importar el saldo.
		assertEquals("VENCIDO",
				LoteResponse.derivarEstado(hoy.minusDays(1), new BigDecimal("10.000")));
		// Agotado: sin saldo disponible y no vencido.
		assertEquals("AGOTADO", LoteResponse.derivarEstado(hoy.plusDays(5), BigDecimal.ZERO));
		// Agotado: disponible negativo.
		assertEquals("AGOTADO", LoteResponse.derivarEstado(hoy.plusDays(5), new BigDecimal("-3.000")));
		// Vigente: con saldo y sin vencer.
		assertEquals("VIGENTE", LoteResponse.derivarEstado(hoy.plusDays(5), new BigDecimal("4.000")));
		// Vigente: sin fecha de vencimiento pero con saldo.
		assertEquals("VIGENTE", LoteResponse.derivarEstado(null, new BigDecimal("4.000")));
		// Vencido tiene prioridad sobre agotado.
		assertEquals("VENCIDO", LoteResponse.derivarEstado(hoy.minusDays(1), BigDecimal.ZERO));
	}

	@Test
	void listarItemsPaginadoBuscaPorQ() {
		stockService.crearItem(new GestionarItem.CrearItemCommand("H1", "Harina 000", "KG", null, null, null));
		stockService.crearItem(new GestionarItem.CrearItemCommand("A1", "Aceite 1L", "UN", null, null, null));

		PageResponse<Item> pagina = stockService.listarItemsPaginado("harina", null, 0, 20);

		assertEquals(1, pagina.content().size());
		assertEquals(1, pagina.totalElements());
		assertEquals(1, pagina.totalPages());
		assertEquals("Harina 000", pagina.content().get(0).getNombre());
	}

	@Test
	void crearItemConCategoriaPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand(
				"SKU-CAT", "Harina Integral", "KG", null, null, "Harinas"));
		assertEquals("Harinas", item.getCategoria());
	}

	@Test
	void listarItemsPaginadoFiltraPorCategoria() {
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-C1", "Harina 000", "KG", null, null, "Harinas"));
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-C2", "Aceite 1L", "UN", null, null, "Aceites"));

		com.sistema.common.model.PageResponse<Item> pagina = stockService.listarItemsPaginado(null, "Harinas", 0, 20);

		assertEquals(1, pagina.content().size());
		assertEquals("SKU-C1", pagina.content().get(0).getSku());
	}

	@Test
	void listarCategoriasDevuelveDistintas() {
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-D1", "A", "UN", null, null, "Harinas"));
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-D2", "B", "UN", null, null, "Harinas"));
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-D3", "C", "UN", null, null, "Aceites"));

		assertEquals(2, stockService.listarCategorias().size());
		assertTrue(stockService.listarCategorias().contains("Harinas"));
		assertTrue(stockService.listarCategorias().contains("Aceites"));
	}

	private static class FakeItemRepository implements ItemRepository {

		private final Map<Long, Item> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Item save(Item item) {
			if (item.getId() == null) {
				item.setId(secuencia.getAndIncrement());
			}
			datos.put(item.getId(), item);
			return item;
		}

		@Override
		public Optional<Item> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Item> findBySku(String sku) {
			return datos.values().stream().filter(i -> i.getSku().equals(sku)).findFirst();
		}

		@Override
		public List<Item> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public PageResponse<Item> buscar(String q, String categoria, int page, int size) {
			return paginarBusqueda(q, categoria, false, page, size);
		}

		@Override
		public PageResponse<Item> buscarActivos(String q, String categoria, int page, int size) {
			return paginarBusqueda(q, categoria, true, page, size);
		}

		private PageResponse<Item> paginarBusqueda(String q, String categoria, boolean soloActivos, int page, int size) {
			String cat = categoria == null || categoria.isBlank() ? null : categoria.trim();
			List<Item> todos = datos.values().stream()
					.filter(i -> !soloActivos || i.isActivo())
					.filter(i -> q == null || q.isBlank()
							|| i.getSku().toLowerCase().contains(q.toLowerCase())
							|| i.getNombre().toLowerCase().contains(q.toLowerCase()))
					.filter(i -> cat == null || cat.equals(i.getCategoria()))
					.toList();
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}

		@Override
		public List<String> listarCategorias() {
			return datos.values().stream()
					.map(Item::getCategoria)
					.filter(c -> c != null && !c.isBlank())
					.distinct()
					.sorted()
					.toList();
		}
	}

	private static class FakeLoteRepository implements LoteRepository {

		private final Map<Long, Lote> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Lote save(Lote lote) {
			if (lote.getId() == null) {
				lote.setId(secuencia.getAndIncrement());
			}
			datos.put(lote.getId(), lote);
			return lote;
		}

		@Override
		public Optional<Lote> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Lote> findByItemId(Long itemId) {
			return datos.values().stream().filter(l -> l.getItemId().equals(itemId)).toList();
		}

		@Override
		public List<Lote> findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(LocalDate fecha) {
			return datos.values().stream()
					.filter(l -> l.getFechaVencimiento() != null && !l.getFechaVencimiento().isAfter(fecha))
					.toList();
		}

		@Override
		public List<Lote> findAll() {
			return new ArrayList<>(datos.values());
		}
	}

	private static class FakeMovimientoStockRepository implements MovimientoStockRepository {

		private final List<MovimientoStock> datos = new ArrayList<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public MovimientoStock save(MovimientoStock movimiento) {
			if (movimiento.getId() == null) {
				movimiento.setId(secuencia.getAndIncrement());
			}
			datos.removeIf(m -> m.getId().equals(movimiento.getId()));
			datos.add(movimiento);
			return movimiento;
		}

		@Override
		public Optional<MovimientoStock> findById(Long id) {
			return datos.stream().filter(m -> m.getId().equals(id)).findFirst();
		}

		@Override
		public List<MovimientoStock> findByItemIdOrderByFechaAsc(Long itemId) {
			return datos.stream()
					.filter(m -> m.getItemId().equals(itemId))
					.sorted(Comparator.comparing(MovimientoStock::getFecha))
					.toList();
		}

		@Override
		public List<MovimientoStock> findByLoteId(Long loteId) {
			return datos.stream().filter(m -> loteId.equals(m.getLoteId())).toList();
		}

		@Override
		public List<MovimientoStock> findByPedidoId(Long pedidoId) {
			return datos.stream().filter(m -> pedidoId.equals(m.getPedidoId())).toList();
		}

		@Override
		public PageResponse<MovimientoStock> listarPaginado(Long itemId, int page, int size) {
			List<MovimientoStock> todos = findByItemIdOrderByFechaAsc(itemId);
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}
	}
}
