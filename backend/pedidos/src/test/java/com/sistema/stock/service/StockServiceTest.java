package com.sistema.stock.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
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
		stockService = new StockService(itemRepository, loteRepository, movimientoRepository);
	}

	private Long itemConIngreso(String sku, BigDecimal cantidad) {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand(sku, "Item " + sku, "UN"));
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
	void mermaMayorAlDisponibleLanzaBusinessException() {
		Long itemId = itemConIngreso("G", new BigDecimal("100.000"));
		Lote lote = loteRepository.findByItemId(itemId).get(0);
		assertThrows(BusinessException.class, () -> stockService.registrarMerma(
				new GestionarMerma.RegistrarMermaCommand(itemId, lote.getId(), new BigDecimal("999.000"), "motivo")));
	}

	@Test
	void ajusteNegativoReduceDisponible() {
		Long itemId = itemConIngreso("H", new BigDecimal("100.000"));
		stockService.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(itemId,
				new BigDecimal("-10.000"), "Diferencia fisica"));

		assertEquals(0, new BigDecimal("90.000").compareTo(stockService.obtenerDisponible(itemId)));
	}

	@Test
	void ajusteInvalidoOLlevaANegativoLanzaBusinessException() {
		Long itemId = itemConIngreso("I", new BigDecimal("100.000"));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, BigDecimal.ZERO, "motivo")));
		assertThrows(BusinessException.class, () -> stockService.ajustarInventario(
				new AjustarInventario.AjusteInventarioCommand(itemId, new BigDecimal("-999.000"), "  ")));
	}

	@Test
	void itemDuplicadoOInexistente() {
		stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-X", "X", "UN"));
		assertThrows(BusinessException.class, () -> stockService.crearItem(
				new GestionarItem.CrearItemCommand("sku-x", "Y", "UN")));
		assertThrows(NotFoundException.class, () -> stockService.desactivarItem(999L));
	}

	@Test
	void desactivarItemPersiste() {
		Item item = stockService.crearItem(new GestionarItem.CrearItemCommand("SKU-Y", "Y", "UN"));
		stockService.desactivarItem(item.getId());
		assertFalse(itemRepository.findById(item.getId()).orElseThrow().isActivo());
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
	}
}
