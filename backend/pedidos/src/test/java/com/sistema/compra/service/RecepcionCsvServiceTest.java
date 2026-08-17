package com.sistema.compra.service;

import com.sistema.common.exception.CsvImportException;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.model.OrdenCompraLinea;
import com.sistema.compra.port.in.ConsultarOrdenCompra;
import com.sistema.compra.port.in.GestionarOrdenCompra;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.port.out.ItemRepository;
import com.sistema.stock.service.IngresoCsvParser;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecepcionCsvServiceTest {

	private RecepcionCsvService service;
	private FakeItemRepository itemRepository;
	private FakeGestionarOrdenCompra gestionar;
	private OrdenCompra orden;

	@BeforeEach
	void setUp() {
		itemRepository = new FakeItemRepository();
		itemRepository.save(item("HAR-001"));
		itemRepository.save(item("ACE-001"));

		OrdenCompraLinea l1 = new OrdenCompraLinea(itemId("HAR-001"), new BigDecimal("100"));
		l1.setId(1L);
		OrdenCompraLinea l2 = new OrdenCompraLinea(itemId("ACE-001"), new BigDecimal("50"));
		l2.setId(2L);
		orden = new OrdenCompra(1L, null);
		orden.setId(10L);
		orden.setNumero("OC-000001");
		orden.agregarLinea(l1);
		orden.agregarLinea(l2);

		gestionar = new FakeGestionarOrdenCompra(orden);
		service = new RecepcionCsvService(new IngresoCsvParser(), itemRepository,
				new FakeConsultarOrdenCompra(orden), gestionar);
	}

	private Item item(String sku) {
		Item i = new Item(sku, "Item " + sku, "UN");
		i.setId(itemId(sku));
		return i;
	}

	private Long itemId(String sku) {
		return (long) sku.hashCode();
	}

	@Test
	void recepcionCsvCreaLotesConPrecioYValidaSkuContraOC() {
		List<Lote> lotes = service.importar(10L,
				"sku,cantidad,precioUnitario\nHAR-001,40,12.50\nACE-001,10,8");

		assertEquals(2, lotes.size());
		GestionarOrdenCompra.RecepcionCsvCommand cmd = gestionar.ultimoCommand;
		assertEquals(10L, cmd.ordenId());
		assertEquals(2, cmd.lineas().size());
		assertEquals(itemId("HAR-001"), cmd.lineas().get(0).itemId());
		assertEquals(0, new BigDecimal("40").compareTo(cmd.lineas().get(0).cantidadRecibida()));
		assertEquals(0, new BigDecimal("12.50").compareTo(cmd.lineas().get(0).precioUnitario()));
	}

	@Test
	void recepcionParcialSoloProcesaLosSkuDelCsv() {
		service.importar(10L, "sku,cantidad,precioUnitario\nHAR-001,30,10");
		assertEquals(1, gestionar.ultimoCommand.lineas().size());
		assertEquals(itemId("HAR-001"), gestionar.ultimoCommand.lineas().get(0).itemId());
	}

	@Test
	void skuInexistenteDevuelveErrorDeFilaSinProcesar() {
		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar(10L, "sku,cantidad,precioUnitario\nHAR-001,10,5\nZZZ,1,1"));
		assertEquals(1, ex.getErrores().size());
		assertTrue(ex.getErrores().get(0).contains("fila 2"));
		assertTrue(ex.getErrores().get(0).contains("ZZZ"));
		assertEquals(null, gestionar.ultimoCommand);
	}

	@Test
	void skuQueNoEstaEnLaOcDevuelveErrorDeFila() {
		itemRepository.save(item("OTRO-001"));
		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar(10L, "sku,cantidad,precioUnitario\nOTRO-001,10,5"));
		assertTrue(ex.getErrores().get(0).contains("no está en la orden"));
	}

	@Test
	void cantidadMayorAlRestanteDevuelveErrorDeFila() {
		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar(10L, "sku,cantidad,precioUnitario\nHAR-001,200,5"));
		assertTrue(ex.getErrores().get(0).contains("supera el restante"));
	}

	@Test
	void ocNoRecibibleLanzaError() {
		orden.setEstado(EstadoOrdenCompra.RECIBIDA);
		CsvImportException ex = assertThrows(CsvImportException.class,
				() -> service.importar(10L, "sku,cantidad,precioUnitario\nHAR-001,10,5"));
		assertTrue(ex.getErrores().get(0).contains("Solo se puede recibir"));
	}

	private static class FakeConsultarOrdenCompra implements ConsultarOrdenCompra {

		private final OrdenCompra orden;

		FakeConsultarOrdenCompra(OrdenCompra orden) {
			this.orden = orden;
		}

		@Override
		public Optional<OrdenCompra> buscarPorId(Long id) {
			return Optional.of(orden);
		}

		@Override
		public List<OrdenCompra> listarTodas() {
			return List.of(orden);
		}

		@Override
		public List<OrdenCompra> listarPorEstado(EstadoOrdenCompra estado) {
			return List.of();
		}

		@Override
		public List<OrdenCompra> listarPorProveedor(Long proveedorId) {
			return List.of();
		}
	}

	private static class FakeGestionarOrdenCompra implements GestionarOrdenCompra {

		private final OrdenCompra orden;
		GestionarOrdenCompra.RecepcionCsvCommand ultimoCommand;

		FakeGestionarOrdenCompra(OrdenCompra orden) {
			this.orden = orden;
		}

		@Override
		public OrdenCompra crearOrdenCompra(CrearOrdenCompraCommand command) {
			return orden;
		}

		@Override
		public OrdenCompra registrarRecepcion(RecepcionCommand command) {
			return orden;
		}

		@Override
		public List<Lote> registrarRecepcionCsv(RecepcionCsvCommand command) {
			ultimoCommand = command;
			List<Lote> lotes = new ArrayList<>();
			int i = 0;
			for (RecepcionCsvLineaCommand linea : command.lineas()) {
				Lote lote = new Lote(linea.itemId(), "LOTE-" + (++i), java.time.LocalDate.now(),
						linea.fechaVencimiento(), linea.cantidadRecibida());
				lote.setPrecioUnitario(linea.precioUnitario());
				lote.setId((long) i);
				lotes.add(lote);
			}
			return lotes;
		}

		@Override
		public void cancelarOrdenCompra(Long ordenId) {
		}
	}

	private static class FakeItemRepository implements ItemRepository {

		private final Map<Long, Item> datos = new HashMap<>();

		@Override
		public Item save(Item item) {
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
}
