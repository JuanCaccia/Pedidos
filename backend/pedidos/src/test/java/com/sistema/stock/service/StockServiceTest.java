package com.sistema.stock.service;

import com.sistema.categoria.model.Categoria;
import com.sistema.categoria.port.out.CategoriaRepository;
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
	private FakeCategoriaRepository categoriaRepository;

	@BeforeEach
	void setUp() {
		itemRepository = new FakeItemRepository();
		loteRepository = new FakeLoteRepository();
		movimientoRepository = new FakeMovimientoStockRepository();
		categoriaRepository = new FakeCategoriaRepository();
		stockService = new StockService(itemRepository, loteRepository, movimientoRepository, categoriaRepository,
				new BigDecimal("50"));
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
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), "LOTE-" + sku, null, cantidad, "Test", null, null));
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
				new BigDecimal("-10.000"), "Diferencia fisica", null, adminActor()));

		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteInvalidoOLlevaANegativoLanzaBusinessException() {
		Long itemId = itemConIngreso("I", new BigDecimal("100.000"));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, BigDecimal.ZERO, "motivo", null, adminActor())));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-999.000"), "  ", null, adminActor())));
	}

	@Test
	void ajusteGrandeConEncargadoLanzaBusinessException() {
		Long itemId = itemConIngreso("I2", new BigDecimal("1000.000"));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-200.000"), "grande", null, encargadoActor())));
	}

	@Test
	void ajusteGrandeConAdminOk() {
		Long itemId = itemConIngreso("I3", new BigDecimal("1000.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-200.000"), "grande", null, adminActor()));
		assertEquals(0, new BigDecimal("800.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteChicoConEncargadoOk() {
		Long itemId = itemConIngreso("I4", new BigDecimal("1000.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "chico", null, encargadoActor()));
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
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-10.000"), "ajuste", null, adminActor())));
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
				new BigDecimal("-10.000"), LocalDateTime.now(), "merma"));

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
	void ingresoConPrecioPersistePrecioEnLote() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-PRECIO", "Precio", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				item.getId(), "LOTE-PRECIO", null, new BigDecimal("100.000"), "Test", null, new BigDecimal("123.45")));

		Lote lote = loteRepository.findByItemId(item.getId()).get(0);
		assertEquals(0, new BigDecimal("123.45").compareTo(lote.getPrecioUnitario()));
	}

	@Test
	void ingresoConPrecioNegativoLanza() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-PRECNEG", "PrecioNeg", "UN", null, null, null));
		assertThrows(BusinessException.class, () -> stockService.crearIngreso(
				new RegistrarIngreso.CrearIngresoCommand(item.getId(), "LOTE-PRECNEG", null, new BigDecimal("100.000"),
						"Test", null, new BigDecimal("-1"))));
	}

	@Test
	void ingresoConProveedorPersisteProveedorIdEnLote() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-PROV", "Prov", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				item.getId(), "LOTE-PROV", null, new BigDecimal("100.000"), "Test", 42L, null));

		Lote lote = loteRepository.findByItemId(item.getId()).get(0);
		assertEquals(42L, lote.getProveedorId());
	}

	@Test
	void ingresoSinProveedorDejaProveedorNullEnLote() {
		Long itemId = itemConIngreso("SINPROV", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		assertEquals(null, lote.getProveedorId());
	}

	@Test
	void listarLotesPorProveedorDevuelveSoloLosDeEseProveedor() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-P2", "P2", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				item.getId(), "LOTE-P2-A", null, new BigDecimal("10.000"), "t", 7L, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				item.getId(), "LOTE-P2-B", null, new BigDecimal("5.000"), "t", 7L, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				item.getId(), "LOTE-P2-C", null, new BigDecimal("3.000"), "t", 9L, null));

		List<Lote> delProveedor7 = stockService.listarLotesPorProveedor(7L);
		List<Lote> delProveedor9 = stockService.listarLotesPorProveedor(9L);

		assertEquals(2, delProveedor7.size());
		assertTrue(delProveedor7.stream().allMatch(l -> l.getProveedorId().equals(7L)));
		assertEquals(1, delProveedor9.size());
		assertTrue(delProveedor9.stream().allMatch(l -> l.getProveedorId().equals(9L)));
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
	void ingresoCreaLoteVigente() {
		Long itemId = itemConIngreso("VIG", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		assertEquals(com.sistema.stock.model.LoteEstado.VIGENTE, lote.getEstado());
	}

	@Test
	void egresoQueAgotaLoteLoDejaAgotado() {
		LocalDate hoy = LocalDate.now();
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("AGOTA", "Agota", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), "L-AGOTA", hoy.plusDays(10),
				new BigDecimal("100.000"), "Test", null, null));
		Lote lote = loteRepository.findByItemId(item.getId()).get(0);

		stockService.egresarPorLotes(item.getId(), 99L, new BigDecimal("100.000"));

		assertEquals(com.sistema.stock.model.LoteEstado.AGOTADO, loteRepository.findById(lote.getId()).get().getEstado());
	}

	@Test
	void egresoParcialNoAgotaLote() {
		LocalDate hoy = LocalDate.now();
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("PARC", "Parc", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), "L-PARC", hoy.plusDays(10),
				new BigDecimal("100.000"), "Test", null, null));
		Lote lote = loteRepository.findByItemId(item.getId()).get(0);

		stockService.egresarPorLotes(item.getId(), 99L, new BigDecimal("40.000"));

		assertEquals(com.sistema.stock.model.LoteEstado.VIGENTE, loteRepository.findById(lote.getId()).get().getEstado());
	}

	@Test
	void descartarLoteConSaldoRegistraMermaYSeteaDescartado() {
		Long itemId = itemConIngreso("DESC", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);

		stockService.descartar(lote.getId());

		Lote persistido = loteRepository.findById(lote.getId()).get();
		assertEquals(com.sistema.stock.model.LoteEstado.DESCARTADO, persistido.getEstado());
		assertEquals(0, new BigDecimal("0.000").compareTo(stockService.obtenerDisponible(itemId)));
		boolean mermaDeDescartes = movimientoRepository.findByItemIdOrderByFechaAsc(itemId).stream()
				.anyMatch(m -> m.getTipo() == TipoMovimiento.MERMA
						&& lote.getId().equals(m.getLoteId())
						&& m.getCantidad().compareTo(new BigDecimal("-100.000")) == 0);
		assertTrue(mermaDeDescartes);
	}

	@Test
	void descartarLoteYaDescartadoRechaza() {
		Long itemId = itemConIngreso("DESC2", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		stockService.descartar(lote.getId());

		assertThrows(BusinessException.class, () -> stockService.descartar(lote.getId()));
	}

	@Test
	void descartarLoteInexistenteLanzaNotFound() {
		assertThrows(NotFoundException.class, () -> stockService.descartar(999L));
	}

	@Test
	void descartarLoteSinSaldoSoloMarcaDescartadoSinMermaAdicional() {
		LocalDate hoy = LocalDate.now();
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("DESC0", "D0", "UN", null, null, null));
		stockService.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(item.getId(), "L-DESC0", hoy.plusDays(10),
				new BigDecimal("50.000"), "Test", null, null));
		Lote lote = loteRepository.findByItemId(item.getId()).get(0);
		stockService.egresarPorLotes(item.getId(), 99L, new BigDecimal("50.000"));
		int mermasAntes = (int) movimientoRepository.findByItemIdOrderByFechaAsc(item.getId()).stream()
				.filter(m -> m.getTipo() == TipoMovimiento.MERMA).count();

		stockService.descartar(lote.getId());

		assertEquals(com.sistema.stock.model.LoteEstado.DESCARTADO, loteRepository.findById(lote.getId()).get().getEstado());
		long mermasDespues = movimientoRepository.findByItemIdOrderByFechaAsc(item.getId()).stream()
				.filter(m -> m.getTipo() == TipoMovimiento.MERMA).count();
		assertEquals(mermasAntes, mermasDespues);
	}

	@Test
	void disponibleDeLoteContemplaAjustePorLote() {
		Long itemId = itemConIngreso("AJL", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		// AUD-010: un ajuste que lleva lote_id impacta el disponible del lote.
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "ajuste por lote", lote.getId(), adminActor()));

		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponibleDeLote(itemId, lote.getId())));
	}

	@Test
	void ajusteGlobalSinLoteNoImpactaDisponibleDeLote() {
		Long itemId = itemConIngreso("AJL2", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "ajuste global", null, adminActor()));

		// Afecta el disponible del item pero no el del lote.
		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponible(itemId)));
		assertEquals(0, new BigDecimal("100.000").compareTo(stockService.obtenerDisponibleDeLote(itemId, lote.getId())));
	}

	@Test
	void ajusteConLoteDeOtroItemLanzaBusinessException() {
		Long itemId = itemConIngreso("AJX", new BigDecimal("100.000"));
		Long otroItemId = itemConIngreso("AJX2", new BigDecimal("100.000"));
		Lote loteDelItem = loteRepository.findByItemId(itemId).get(0);

		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(otroItemId, new BigDecimal("-5.000"),
						"ajuste", loteDelItem.getId(), adminActor())));
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
		Categoria harinas = categoriaRepository.save(new Categoria("Harinas"));
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand(
				"SKU-CAT", "Harina Integral", "KG", null, null, harinas.getId()));
		assertEquals(harinas.getId(), item.getCategoriaId());
		assertEquals("Harinas", item.getCategoriaNombre());
	}

	@Test
	void crearItemConCategoriaInexistenteLanza() {
		assertThrows(BusinessException.class, () -> stockService.crearItem(
				new GestionarItem.CrearItemCommand("SKU-CATX", "X", "UN", null, null, 999L)));
	}

	@Test
	void listarItemsPaginadoFiltraPorCategoria() {
		Categoria harinas = categoriaRepository.save(new Categoria("Harinas"));
		Categoria aceites = categoriaRepository.save(new Categoria("Aceites"));
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-C1", "Harina 000", "KG", null, null, harinas.getId()));
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-C2", "Aceite 1L", "UN", null, null, aceites.getId()));

		com.sistema.common.model.PageResponse<Item> pagina = stockService.listarItemsPaginado(null, harinas.getId(), 0, 20);

		assertEquals(1, pagina.content().size());
		assertEquals("SKU-C1", pagina.content().get(0).getSku());
		assertEquals("Harinas", pagina.content().get(0).getCategoriaNombre());
	}

	@Test
	void listarCategoriasDevuelveActivas() {
		categoriaRepository.save(new Categoria("Harinas"));
		categoriaRepository.save(new Categoria("Aceites"));
		Categoria inactiva = categoriaRepository.save(new Categoria("Congelados"));
		categoriaRepository.save(inactiva, false);

		assertEquals(2, stockService.listarCategorias().size());
		assertTrue(stockService.listarCategorias().contains("Harinas"));
		assertTrue(stockService.listarCategorias().contains("Aceites"));
		assertFalse(stockService.listarCategorias().contains("Congelados"));
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
		public PageResponse<Item> buscar(String q, Long categoriaId, int page, int size) {
			return paginarBusqueda(q, categoriaId, false, page, size);
		}

		@Override
		public PageResponse<Item> buscarActivos(String q, Long categoriaId, int page, int size) {
			return paginarBusqueda(q, categoriaId, true, page, size);
		}

		private PageResponse<Item> paginarBusqueda(String q, Long categoriaId, boolean soloActivos, int page, int size) {
			List<Item> todos = datos.values().stream()
					.filter(i -> !soloActivos || i.isActivo())
					.filter(i -> q == null || q.isBlank()
							|| i.getSku().toLowerCase().contains(q.toLowerCase())
							|| i.getNombre().toLowerCase().contains(q.toLowerCase()))
					.filter(i -> categoriaId == null || categoriaId.equals(i.getCategoriaId()))
					.toList();
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}
	}

	private static class FakeCategoriaRepository implements CategoriaRepository {

		private final Map<Long, Categoria> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Categoria save(Categoria categoria) {
			if (categoria.getId() == null) {
				categoria.setId(secuencia.getAndIncrement());
			}
			datos.put(categoria.getId(), categoria);
			return categoria;
		}

		public Categoria save(Categoria categoria, boolean activo) {
			categoria.setActivo(activo);
			return save(categoria);
		}

		@Override
		public Optional<Categoria> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Categoria> findByNombre(String nombre) {
			return datos.values().stream().filter(c -> c.getNombre().equals(nombre)).findFirst();
		}

		@Override
		public List<Categoria> findAll() {
			return datos.values().stream().sorted(Comparator.comparing(Categoria::getNombre)).toList();
		}

		@Override
		public List<Categoria> findByActivoTrue() {
			return findAll().stream().filter(Categoria::isActivo).toList();
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
		public List<Lote> findByProveedorId(Long proveedorId) {
			return datos.values().stream().filter(l -> proveedorId.equals(l.getProveedorId())).toList();
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
