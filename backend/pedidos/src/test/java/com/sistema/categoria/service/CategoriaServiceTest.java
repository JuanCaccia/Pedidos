package com.sistema.categoria.service;

import com.sistema.categoria.model.Categoria;
import com.sistema.categoria.port.in.GestionarCategoria;
import com.sistema.categoria.port.out.CategoriaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaServiceTest {

	private CategoriaService categoriaService;
	private FakeCategoriaRepository repository;

	@BeforeEach
	void setUp() {
		repository = new FakeCategoriaRepository();
		categoriaService = new CategoriaService(repository);
	}

	@Test
	void crearPersisteYNormalizaNombre() {
		Categoria creada = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("  Harinas  "));
		assertEquals("Harinas", creada.getNombre());
		assertTrue(creada.isActivo());
		assertEquals("Harinas", repository.findById(creada.getId()).orElseThrow().getNombre());
	}

	@Test
	void crearNombreVacioOLargoLanza() {
		assertThrows(BusinessException.class,
				() -> categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("   ")));
		assertThrows(BusinessException.class,
				() -> categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("a".repeat(101))));
	}

	@Test
	void crearDuplicadoRechazado() {
		categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		assertThrows(BusinessException.class,
				() -> categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("  Harinas  ")));
	}

	@Test
	void actualizarRenombra() {
		Categoria creada = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		Categoria actualizada = categoriaService.actualizar(
				new GestionarCategoria.ActualizarCategoriaCommand(creada.getId(), "Harinas 000"));
		assertEquals("Harinas 000", actualizada.getNombre());
		assertEquals("Harinas 000", repository.findById(creada.getId()).orElseThrow().getNombre());
	}

	@Test
	void actualizarADuplicadoRechazado() {
		categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		Categoria otra = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Aceites"));
		assertThrows(BusinessException.class, () -> categoriaService.actualizar(
				new GestionarCategoria.ActualizarCategoriaCommand(otra.getId(), "Harinas")));
	}

	@Test
	void actualizarInexistenteLanza() {
		assertThrows(NotFoundException.class, () -> categoriaService.actualizar(
				new GestionarCategoria.ActualizarCategoriaCommand(999L, "X")));
	}

	@Test
	void desactivarYReactivar() {
		Categoria creada = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		categoriaService.desactivar(creada.getId());
		assertFalse(repository.findById(creada.getId()).orElseThrow().isActivo());
		assertTrue(categoriaService.listarActivas().isEmpty());

		categoriaService.reactivar(creada.getId());
		assertTrue(repository.findById(creada.getId()).orElseThrow().isActivo());
		assertEquals(1, categoriaService.listarActivas().size());
	}

	@Test
	void desactivarInexistenteLanza() {
		assertThrows(NotFoundException.class, () -> categoriaService.desactivar(999L));
	}

	@Test
	void listarActivasExcluyeInactivas() {
		categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		Categoria congelados = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Congelados"));
		categoriaService.desactivar(congelados.getId());

		List<Categoria> activas = categoriaService.listarActivas();
		assertEquals(1, activas.size());
		assertEquals("Harinas", activas.get(0).getNombre());
	}

	@Test
	void listarTodasIncluyeInactivasOrdenadas() {
		categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Zapateria"));
		Categoria congelados = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Aceites"));
		categoriaService.desactivar(congelados.getId());

		List<Categoria> todas = categoriaService.listarTodas();
		assertEquals(2, todas.size());
		assertEquals("Aceites", todas.get(0).getNombre());
		assertEquals("Zapateria", todas.get(1).getNombre());
	}

	@Test
	void buscarPorIdDevuelve() {
		Categoria creada = categoriaService.crear(new GestionarCategoria.CrearCategoriaCommand("Harinas"));
		assertTrue(categoriaService.buscarPorId(creada.getId()).isPresent());
		assertTrue(categoriaService.buscarPorId(999L).isEmpty());
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
}
