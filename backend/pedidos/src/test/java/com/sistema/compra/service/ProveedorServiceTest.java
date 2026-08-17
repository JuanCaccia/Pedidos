package com.sistema.compra.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.model.ProveedorItem;
import com.sistema.compra.port.in.GestionarProveedor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProveedorServiceTest {

	private ProveedorService proveedorService;
	private FakeProveedorRepository proveedorRepository;
	private FakeStockGateway stockGateway;

	@BeforeEach
	void setUp() {
		proveedorRepository = new FakeProveedorRepository();
		stockGateway = new FakeStockGateway();
		proveedorService = new ProveedorService(proveedorRepository, stockGateway);
	}

	@Test
	void crearProveedorPersiste() {
		Proveedor proveedor = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", " 20123456789 ", "a@b.com", "555-1234"));

		assertEquals("Distribuidora S.A.", proveedor.getRazonSocial());
		assertEquals("20123456789", proveedor.getCuit());
		assertEquals("a@b.com", proveedor.getEmail());
		assertEquals("555-1234", proveedor.getTelefono());
		assertTrue(proveedor.isActivo());
		assertEquals(1, proveedorRepository.findAll().size());
	}

	@Test
	void cuitDuplicadoLanzaBusinessException() {
		proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"A", "20123456789", null, null));

		assertThrows(BusinessException.class, () -> proveedorService.crearProveedor(
				new GestionarProveedor.CrearProveedorCommand("B", "20123456789", null, null)));
	}

	@Test
	void cuitInvalidoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> proveedorService.crearProveedor(
				new GestionarProveedor.CrearProveedorCommand("B", "123", null, null)));
	}

	@Test
	void actualizarProveedorCambiaDatos() {
		Proveedor creado = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", "a@b.com", null));

		Proveedor actualizado = proveedorService.actualizarProveedor(new GestionarProveedor.ActualizarProveedorCommand(
				creado.getId(), "Distribuidora Renombrada S.A.", "nuevo@b.com", "555-9999"));

		assertEquals("Distribuidora Renombrada S.A.", actualizado.getRazonSocial());
		assertEquals("nuevo@b.com", actualizado.getEmail());
		assertEquals("555-9999", actualizado.getTelefono());
		assertEquals("20123456789", actualizado.getCuit());
	}

	@Test
	void actualizarProveedorInexistenteLanzaNotFoundException() {
		assertThrows(NotFoundException.class, () -> proveedorService.actualizarProveedor(
				new GestionarProveedor.ActualizarProveedorCommand(999L, "X", null, null)));
	}

	@Test
	void desactivarYReactivar() {
		Proveedor creado = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));

		proveedorService.desactivarProveedor(creado.getId());
		Proveedor desactivado = proveedorRepository.findById(creado.getId()).orElseThrow();
		assertFalse(desactivado.isActivo());

		proveedorService.reactivarProveedor(creado.getId());
		Proveedor reactivado = proveedorRepository.findById(creado.getId()).orElseThrow();
		assertTrue(reactivado.isActivo());
	}

	@Test
	void setItemsDeProveedorVinculaCatalogoCompleto() {
		Proveedor proveedor = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));

		List<ProveedorItem> items = proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(proveedor.getId(), List.of(10L, 20L, 30L)));

		assertEquals(3, items.size());
		assertTrue(proveedorRepository.proveedorProveeItemActivo(proveedor.getId(), 10L));
		assertTrue(proveedorRepository.proveedorProveeItemActivo(proveedor.getId(), 30L));
	}

	@Test
	void setItemsDeProveedorReemplazaCatalogoAnterior() {
		Proveedor proveedor = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));
		proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(proveedor.getId(), List.of(10L, 20L)));

		proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(proveedor.getId(), List.of(20L, 40L)));

		List<ProveedorItem> items = proveedorService.listarItemsDeProveedor(proveedor.getId(), true);
		assertEquals(2, items.size());
		assertFalse(proveedorRepository.proveedorProveeItemActivo(proveedor.getId(), 10L));
		assertTrue(proveedorRepository.proveedorProveeItemActivo(proveedor.getId(), 40L));
	}

	@Test
	void setItemsDeProveedorInexistenteLanzaNotFound() {
		assertThrows(NotFoundException.class, () -> proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(999L, List.of(10L))));
	}

	@Test
	void setItemsDeProveedorConItemInexistenteLanzaNotFound() {
		Proveedor proveedor = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));

		assertThrows(NotFoundException.class, () -> proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(proveedor.getId(), List.of(10L, 999L))));
		assertFalse(proveedorRepository.proveedorProveeItemActivo(proveedor.getId(), 10L));
	}

	@Test
	void listarItemsDeProveedorDevuelveItemsVinculados() {
		Proveedor proveedor = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));
		proveedorService.setItemsDeProveedor(
				new GestionarProveedor.SetItemsCommand(proveedor.getId(), List.of(10L, 20L)));

		List<ProveedorItem> items = proveedorService.listarItemsDeProveedor(proveedor.getId(), true);

		assertEquals(2, items.size());
		assertTrue(items.stream().allMatch(i -> i.getProveedorId().equals(proveedor.getId())));
		assertTrue(items.stream().anyMatch(i -> i.getItemId().equals(10L)));
	}

	@Test
	void listarItemsDeProveedorInexistenteLanzaNotFound() {
		assertThrows(NotFoundException.class, () -> proveedorService.listarItemsDeProveedor(999L, true));
	}

	@Test
	void listarProveedoresDeItemDevuelveQuienesLoOfrecen() {
		Proveedor p1 = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Distribuidora S.A.", "20123456789", null, null));
		Proveedor p2 = proveedorService.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				"Otra S.A.", "20234567890", null, null));
		proveedorService.setItemsDeProveedor(new GestionarProveedor.SetItemsCommand(p1.getId(), List.of(10L)));
		proveedorService.setItemsDeProveedor(new GestionarProveedor.SetItemsCommand(p2.getId(), List.of(10L, 20L)));

		List<Proveedor> proveedores = proveedorService.listarProveedoresDeItem(10L);

		assertEquals(2, proveedores.size());
		assertTrue(proveedores.stream().anyMatch(p -> p.getId().equals(p1.getId())));
		assertTrue(proveedores.stream().anyMatch(p -> p.getId().equals(p2.getId())));
	}

	private static class FakeProveedorRepository implements ProveedorRepository {

		private final Map<Long, Proveedor> datos = new HashMap<>();
		private final Map<Long, Set<Long>> itemsPorProveedor = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

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

		private final Set<Long> existentes = new LinkedHashSet<>(List.of(10L, 20L, 30L, 40L, 100L, 200L));

		@Override
		public boolean existeItem(Long itemId) {
			return existentes.contains(itemId);
		}

		@Override
		public boolean itemActivo(Long itemId) {
			return true;
		}

		@Override
		public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo, Long proveedorId,
				BigDecimal precioUnitario) {
			// no-op en este test
		}
	}
}
