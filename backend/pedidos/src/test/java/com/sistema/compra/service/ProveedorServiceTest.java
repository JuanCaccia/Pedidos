package com.sistema.compra.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.port.in.GestionarProveedor;
import com.sistema.compra.port.out.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProveedorServiceTest {

	private ProveedorService proveedorService;
	private FakeProveedorRepository proveedorRepository;

	@BeforeEach
	void setUp() {
		proveedorRepository = new FakeProveedorRepository();
		proveedorService = new ProveedorService(proveedorRepository);
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

	private static class FakeProveedorRepository implements ProveedorRepository {

		private final Map<Long, Proveedor> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

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
	}
}
