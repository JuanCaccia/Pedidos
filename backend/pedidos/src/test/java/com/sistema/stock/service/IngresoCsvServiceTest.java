package com.sistema.stock.service;

import com.sistema.common.exception.CsvImportException;
import com.sistema.common.model.PageResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.port.out.ItemRepository;
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

class IngresoCsvServiceTest {

	private IngresoCsvService service;
	private FakeItemRepository itemRepository;
	private FakeRegistrarIngreso registrar;

	@BeforeEach
	void setUp() {
		itemRepository = new FakeItemRepository();
		registrar = new FakeRegistrarIngreso();
		service = new IngresoCsvService(new IngresoCsvParser(), itemRepository, registrar);
	}

	@Test
	void creaLotesYMovimientosConPrecio() {
		itemRepository.save(new Item("HAR-001", "Harina", "KG"));
		itemRepository.save(new Item("ACE-001", "Aceite", "UN"));

		List<Lote> lotes = service.importar("sku,cantidad,precioUnitario\nHAR-001,100,12.50\nACE-001,5,30", null);

		assertEquals(2, lotes.size());
		assertEquals(2, registrar.lotes.size());
		Lote l1 = registrar.lotes.get(0);
		assertEquals(0, new BigDecimal("100").compareTo(l1.getCantidadIngresada()));
		assertEquals(0, new BigDecimal("12.50").compareTo(l1.getPrecioUnitario()));
		Lote l2 = registrar.lotes.get(1);
		assertEquals(0, new BigDecimal("30").compareTo(l2.getPrecioUnitario()));
	}

	@Test
	void vinculaProveedorOpcionalAlLote() {
		itemRepository.save(new Item("HAR-001", "Harina", "KG"));
		service.importar("sku,cantidad,precioUnitario\nHAR-001,10,5", 42L);
		assertEquals(42L, registrar.lotes.get(0).getProveedorId());
	}

	@Test
	void skuInexistenteLanzaErrorAgregadoSinProcesarNada() {
		itemRepository.save(new Item("HAR-001", "Harina", "KG"));

		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar("sku,cantidad,precioUnitario\nHAR-001,10,5\nZZZ,1,1", null));

		assertEquals(1, ex.getErrores().size());
		assertTrue(ex.getErrores().get(0).contains("fila 2"));
		assertTrue(ex.getErrores().get(0).contains("ZZZ"));
		assertTrue(registrar.lotes.isEmpty());
	}

	@Test
	void itemInactivoLanzaError() {
		Item item = new Item("HAR-001", "Harina", "KG");
		item.setActivo(false);
		itemRepository.save(item);

		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar("sku,cantidad,precioUnitario\nHAR-001,10,5", null));
		assertTrue(ex.getErrores().get(0).contains("inactivo"));
	}

	@Test
	void skuConDistintaCajaResuelveIgual() {
		itemRepository.save(new Item("HAR-001", "Harina", "KG"));
		service.importar("sku,cantidad,precioUnitario\nhar-001,10,5", null);
		assertEquals(1, registrar.lotes.size());
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
			return new PageResponse<>(findAll(), page, size, datos.size(), 1);
		}

		@Override
		public PageResponse<Item> buscarActivos(String q, Long categoriaId, int page, int size) {
			return buscar(q, categoriaId, page, size);
		}
	}

	private static class FakeRegistrarIngreso implements RegistrarIngreso {

		private final List<Lote> lotes = new ArrayList<>();

		@Override
		public Lote crearIngreso(CrearIngresoCommand command) {
			Lote lote = new Lote(command.itemId(), command.codigoLote(), LocalDate.now(),
					command.fechaVencimiento(), command.cantidad());
			lote.setProveedorId(command.proveedorId());
			lote.setPrecioUnitario(command.precioUnitario());
			lote.setId((long) lotes.size() + 1);
			lotes.add(lote);
			return lote;
		}
	}
}
