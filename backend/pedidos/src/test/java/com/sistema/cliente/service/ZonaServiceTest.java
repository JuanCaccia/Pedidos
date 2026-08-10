package com.sistema.cliente.service;

import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.model.Zona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZonaServiceTest {

	private ZonaService zonaService;
	private FakeZonaRepository repository;

	@BeforeEach
	void setUp() {
		repository = new FakeZonaRepository();
		zonaService = new ZonaService(repository);
	}

	@Test
	void crearZonaPersiste() {
		Zona zona = zonaService.crearZona(new GestionarZona.CrearZonaCommand("Zona Norte"));
		assertEquals("Zona Norte", zona.getNombre());
		assertEquals(1, repository.findAll().size());
	}

	@Test
	void crearZonaDuplicadaLanzaBusinessException() {
		zonaService.crearZona(new GestionarZona.CrearZonaCommand("Zona Norte"));
		assertThrows(BusinessException.class,
				() -> zonaService.crearZona(new GestionarZona.CrearZonaCommand("Zona Norte")));
	}

	@Test
	void crearZonaSinNombreLanzaBusinessException() {
		assertThrows(BusinessException.class,
				() -> zonaService.crearZona(new GestionarZona.CrearZonaCommand("  ")));
	}

	private static class FakeZonaRepository implements ZonaRepository {

		private final Map<Long, Zona> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Zona save(Zona zona) {
			if (zona.getId() == null) {
				zona.setId(secuencia.getAndIncrement());
			}
			datos.put(zona.getId(), zona);
			return zona;
		}

		@Override
		public Optional<Zona> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Zona> findByNombre(String nombre) {
			return datos.values().stream().filter(z -> z.getNombre().equals(nombre)).findFirst();
		}

		@Override
		public List<Zona> findAll() {
			return new ArrayList<>(datos.values());
		}
	}
}
