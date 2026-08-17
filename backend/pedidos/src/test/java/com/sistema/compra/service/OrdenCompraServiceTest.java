package com.sistema.compra.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.model.OrdenCompraLinea;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;
import com.sistema.compra.port.in.GestionarOrdenCompra;
import com.sistema.compra.port.out.OrdenCompraRepository;
import com.sistema.compra.port.out.ProveedorRepository;
import com.sistema.compra.port.out.StockGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

class OrdenCompraServiceTest {

	private OrdenCompraService ordenCompraService;
	private FakeOrdenCompraRepository ordenCompraRepository;
	private FakeProveedorRepository proveedorRepository;
	private FakeStockGateway stockGateway;

	@BeforeEach
	void setUp() {
		ordenCompraRepository = new FakeOrdenCompraRepository();
		proveedorRepository = new FakeProveedorRepository();
		Proveedor proveedor = proveedorRepository.save(new Proveedor("Distribuidora S.A.", "20111111111"));
		proveedorRepository.vincularItem(proveedor.getId(), 100L);
		proveedorRepository.vincularItem(proveedor.getId(), 200L);
		stockGateway = new FakeStockGateway();
		ordenCompraService = new OrdenCompraService(ordenCompraRepository, proveedorRepository, stockGateway);
	}

	private GestionarOrdenCompra.CrearOrdenCompraCommand comandoConDosLineas() {
		return new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, "obs",
				List.of(new GestionarOrdenCompra.LineaOrdenCommand(100L, new BigDecimal("10"), new BigDecimal("50")),
						new GestionarOrdenCompra.LineaOrdenCommand(200L, new BigDecimal("5"), new BigDecimal("30"))));
	}

	private GestionarOrdenCompra.CrearOrdenCompraCommand comandoConUnaLinea() {
		return new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, "obs",
				List.of(new GestionarOrdenCompra.LineaOrdenCommand(100L, new BigDecimal("10"), new BigDecimal("50"))));
	}

	@Test
	void crearOrdenCompraPersisteConLineas() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConDosLineas());

		assertNotNull(orden.getId());
		assertTrue(orden.getNumero().startsWith("OC-"));
		assertEquals(EstadoOrdenCompra.PENDIENTE, orden.getEstado());
		assertEquals(2, orden.getLineas().size());
		assertNotNull(orden.getLineas().get(0).getId());
		assertNotNull(orden.getLineas().get(1).getId());
		assertEquals(1, ordenCompraRepository.findAll().size());
	}

	@Test
	void crearOrdenCompraSinLineasLanza() {
		assertThrows(BusinessException.class, () -> ordenCompraService.crearOrdenCompra(
				new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, null, List.of())));
	}

	@Test
	void crearOrdenCompraConProveedorInexistenteLanza() {
		assertThrows(NotFoundException.class, () -> ordenCompraService.crearOrdenCompra(
				new GestionarOrdenCompra.CrearOrdenCompraCommand(999L, null, List.of(
						new GestionarOrdenCompra.LineaOrdenCommand(100L, new BigDecimal("10"), new BigDecimal("50"))))));
	}

	@Test
	void crearOrdenCompraConItemInexistenteLanza() {
		stockGateway.existe = false;
		assertThrows(NotFoundException.class, () -> ordenCompraService.crearOrdenCompra(
				new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, null, List.of(
						new GestionarOrdenCompra.LineaOrdenCommand(999L, new BigDecimal("10"), new BigDecimal("50"))))));
	}

	@Test
	void crearOrdenCompraConItemInactivoLanzaBusinessException() {
		stockGateway.itemActivo = false;
		assertThrows(BusinessException.class, () -> ordenCompraService.crearOrdenCompra(
				new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, null, List.of(
						new GestionarOrdenCompra.LineaOrdenCommand(100L, new BigDecimal("10"), new BigDecimal("50"))))));
	}

	@Test
	void crearOrdenCompraConItemNoProvistoPorProveedorLanzaBusinessException() {
		BusinessException ex = assertThrows(BusinessException.class, () -> ordenCompraService.crearOrdenCompra(
				new GestionarOrdenCompra.CrearOrdenCompraCommand(1L, null, List.of(
						new GestionarOrdenCompra.LineaOrdenCommand(999L, new BigDecimal("10"), new BigDecimal("50"))))));
		assertEquals("ITEM_NO_PROVISTO_POR_PROVEEDOR", ex.getCode());
	}

	@Test
	void crearOrdenCompraConProveedorInactivoLanzaBusinessException() {
		Proveedor proveedor = proveedorRepository.findById(1L).orElseThrow();
		proveedor.desactivar();
		proveedorRepository.save(proveedor);

		assertThrows(BusinessException.class, () -> ordenCompraService.crearOrdenCompra(comandoConUnaLinea()));
	}

	@Test
	void recepcionParcialRegistraIngresoYQuedaParcial() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConDosLineas());
		Long linea1 = orden.getLineas().get(0).getId();
		Long linea2 = orden.getLineas().get(1).getId();

		OrdenCompra recibida = ordenCompraService.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(
				orden.getId(), List.of(
						new GestionarOrdenCompra.RecepcionLineaCommand(linea1, new BigDecimal("10")),
						new GestionarOrdenCompra.RecepcionLineaCommand(linea2, new BigDecimal("3")))));

		assertEquals(EstadoOrdenCompra.RECIBIDA_PARCIAL, recibida.getEstado());
		assertEquals(2, stockGateway.ingresos.size());
		assertTrue(stockGateway.ingresos.contains("100:10:1"));
		assertTrue(stockGateway.ingresos.contains("200:3:1"));
	}

	@Test
	void recepcionCompletaQuedaRecibida() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConUnaLinea());
		Long linea = orden.getLineas().get(0).getId();

		OrdenCompra recibida = ordenCompraService.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(
				orden.getId(), List.of(
						new GestionarOrdenCompra.RecepcionLineaCommand(linea, new BigDecimal("10")))));

		assertEquals(EstadoOrdenCompra.RECIBIDA, recibida.getEstado());
		assertEquals(1, stockGateway.ingresos.size());
	}

	@Test
	void recepcionExcedeRestanteLanza() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConUnaLinea());
		Long linea = orden.getLineas().get(0).getId();

		assertThrows(BusinessException.class, () -> ordenCompraService.registrarRecepcion(
				new GestionarOrdenCompra.RecepcionCommand(orden.getId(), List.of(
						new GestionarOrdenCompra.RecepcionLineaCommand(linea, new BigDecimal("12"))))));
		assertEquals(0, stockGateway.ingresos.size());
	}

	@Test
	void recepcionAsociaProveedorDeLaOcAlIngresoDeLote() {
		Proveedor segundoProveedor = new Proveedor("Otra Distribuidora S.A.", "20222222222");
		segundoProveedor = proveedorRepository.save(segundoProveedor);
		proveedorRepository.vincularItem(segundoProveedor.getId(), 100L);
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(new GestionarOrdenCompra.CrearOrdenCompraCommand(
				segundoProveedor.getId(), "obs",
				List.of(new GestionarOrdenCompra.LineaOrdenCommand(100L, new BigDecimal("10"), new BigDecimal("50")))));
		Long linea = orden.getLineas().get(0).getId();

		ordenCompraService.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(orden.getId(), List.of(
				new GestionarOrdenCompra.RecepcionLineaCommand(linea, new BigDecimal("10")))));

		assertEquals(1, stockGateway.ingresos.size());
		assertTrue(stockGateway.ingresos.contains("100:10:" + segundoProveedor.getId()));
	}

	@Test
	void recepcionAutoVinculaItemsRecibidosAlProveedor() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConUnaLinea());
		Long linea = orden.getLineas().get(0).getId();
		proveedorRepository.reemplazarItems(1L, List.of());
		assertFalse(proveedorRepository.proveedorProveeItemActivo(1L, 100L));

		ordenCompraService.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(orden.getId(), List.of(
				new GestionarOrdenCompra.RecepcionLineaCommand(linea, new BigDecimal("10")))));

		assertTrue(proveedorRepository.proveedorProveeItemActivo(1L, 100L));
	}

	@Test
	void cancelarOrdenCompraDesdePendiente() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConUnaLinea());

		ordenCompraService.cancelarOrdenCompra(orden.getId());

		OrdenCompra cancelada = ordenCompraRepository.findById(orden.getId()).orElseThrow();
		assertEquals(EstadoOrdenCompra.CANCELADA, cancelada.getEstado());
	}

	@Test
	void cancelarRecibidaLanza() {
		OrdenCompra orden = ordenCompraService.crearOrdenCompra(comandoConUnaLinea());
		Long linea = orden.getLineas().get(0).getId();
		ordenCompraService.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(orden.getId(), List.of(
				new GestionarOrdenCompra.RecepcionLineaCommand(linea, new BigDecimal("10")))));

		assertThrows(BusinessException.class, () -> ordenCompraService.cancelarOrdenCompra(orden.getId()));
	}

	@Test
	void cancelarOrdenCompraInexistenteLanza() {
		assertThrows(NotFoundException.class, () -> ordenCompraService.cancelarOrdenCompra(999L));
	}

	private static class FakeOrdenCompraRepository implements OrdenCompraRepository {

		private final Map<Long, OrdenCompra> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);
		private final AtomicLong secuenciaLinea = new AtomicLong(1);

		@Override
		public OrdenCompra save(OrdenCompra orden) {
			if (orden.getId() == null) {
				orden.setId(secuencia.getAndIncrement());
			}
			for (OrdenCompraLinea linea : orden.getLineas()) {
				if (linea.getId() == null) {
					linea.setId(secuenciaLinea.getAndIncrement());
				}
			}
			datos.put(orden.getId(), orden);
			return orden;
		}

		@Override
		public Optional<OrdenCompra> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<OrdenCompra> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public List<OrdenCompra> findByEstado(EstadoOrdenCompra estado) {
			return datos.values().stream().filter(o -> o.getEstado() == estado).toList();
		}

		@Override
		public List<OrdenCompra> findByProveedorId(Long proveedorId) {
			return datos.values().stream().filter(o -> o.getProveedorId().equals(proveedorId)).toList();
		}
	}

	private static class FakeProveedorRepository implements ProveedorRepository {

		private final Map<Long, Proveedor> datos = new HashMap<>();
		private final Map<Long, Set<Long>> itemsPorProveedor = new HashMap<>();
		private final 		AtomicLong secuencia = new AtomicLong(1);

		@Override
		public void vincularItem(Long proveedorId, Long itemId) {
			itemsPorProveedor.computeIfAbsent(proveedorId, k -> new LinkedHashSet<>()).add(itemId);
		}

		@Override
		public Proveedor save(Proveedor proveedor) {
			if (proveedor.getId() == null) {
				proveedor.setId(secuencia.getAndIncrement());
			}
			datos.put(proveedor.getId(), proveedor);
			return proveedor;
		}

		@Override
		public Optional<Proveedor> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Proveedor> findByCuit(String cuit) {
			return datos.values().stream().filter(p -> p.getCuit().equals(cuit)).findFirst();
		}

		@Override
		public List<Proveedor> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public PageResponse<Proveedor> buscar(String q, int page, int size) {
			List<Proveedor> todos = datos.values().stream()
					.filter(p -> q == null || q.isBlank()
							|| p.getRazonSocial().toLowerCase().contains(q.toLowerCase())
							|| p.getCuit().toLowerCase().contains(q.toLowerCase()))
					.toList();
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}

		@Override
		public void reemplazarItems(Long proveedorId, List<Long> itemIds) {
			itemsPorProveedor.put(proveedorId, new LinkedHashSet<>(itemIds));
		}

		@Override
		public List<ProveedorItem> listarItemsDeProveedor(Long proveedorId, boolean soloActivos) {
			return itemsPorProveedor.getOrDefault(proveedorId, Set.of()).stream()
					.map(itemId -> {
						ProveedorItem pi = new ProveedorItem(proveedorId, itemId);
						pi.setItemSku("SKU-" + itemId);
						pi.setItemNombre("Item " + itemId);
						return pi;
					})
					.toList();
		}

		@Override
		public boolean proveedorProveeItemActivo(Long proveedorId, Long itemId) {
			return itemsPorProveedor.getOrDefault(proveedorId, Set.of()).contains(itemId);
		}

		@Override
		public List<Proveedor> listarProveedoresDeItem(Long itemId) {
			return datos.values().stream()
					.filter(p -> itemsPorProveedor.getOrDefault(p.getId(), Set.of()).contains(itemId))
					.toList();
		}
	}

	private static class FakeStockGateway implements StockGateway {

		private final List<String> ingresos = new ArrayList<>();
		private boolean existe = true;
		private boolean itemActivo = true;

		@Override
		public boolean existeItem(Long itemId) {
			return existe;
		}

		@Override
		public boolean itemActivo(Long itemId) {
			return itemActivo;
		}

		@Override
		public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo, Long proveedorId) {
			ingresos.add(itemId + ":" + cantidad + ":" + proveedorId);
		}
	}
}
