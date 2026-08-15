package com.sistema.cliente.service;

import com.sistema.cliente.model.Cliente;
import com.sistema.cliente.port.in.GestionarCliente;
import com.sistema.cliente.port.out.ClienteRepository;
import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteServiceTest {

	private ClienteService clienteService;
	private FakeClienteRepository clienteRepository;

	@BeforeEach
	void setUp() {
		clienteRepository = new FakeClienteRepository();
		FakeZonaRepository zonaRepository = new FakeZonaRepository();
		Zona zonaCentro = new Zona("Zona Centro");
		zonaCentro.setId(1L);
		zonaRepository.datos.put(1L, zonaCentro);
		clienteService = new ClienteService(clienteRepository, zonaRepository);
	}

	@Test
	void listarPaginadoDivideEnPaginas() {
		clienteService.crearCliente(new GestionarCliente.CrearClienteCommand("A S.A.", "20111111111", null, null, null, 1L));
		clienteService.crearCliente(new GestionarCliente.CrearClienteCommand("B S.A.", "20222222222", null, null, null, 1L));
		clienteService.crearCliente(new GestionarCliente.CrearClienteCommand("C S.A.", "20333333333", null, null, null, 1L));

		PageResponse<Cliente> pagina = clienteService.listarPaginado(null, null, 0, 2);

		assertEquals(2, pagina.content().size());
		assertEquals(3, pagina.totalElements());
		assertEquals(2, pagina.totalPages());
	}

	@Test
	void crearClienteConZonaValidaPersiste() {
		Cliente cliente = clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"Empresa S.A.", "20123456789", "a@b.com", "555-1234", "Calle 1", 1L));

		assertEquals("Empresa S.A.", cliente.getRazonSocial());
		assertEquals("20123456789", cliente.getCuit());
		assertEquals("Zona Centro", cliente.getZona().getNombre());
		assertTrue(cliente.isActivo());
		assertEquals(1, clienteRepository.findAll().size());
	}

	@Test
	void crearClienteCuitDuplicadoLanzaBusinessException() {
		clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"A", "20123456789", null, null, null, 1L));
		assertThrows(BusinessException.class, () -> clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"B", "20123456789", null, null, null, 1L)));
	}

	@Test
	void crearClienteCuitInvalidoLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"B", "123", null, null, null, 1L)));
	}

	@Test
	void crearClienteZonaInexistenteLanzaNotFoundException() {
		assertThrows(NotFoundException.class, () -> clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"B", "20111112222", null, null, null, 999L)));
	}

	@Test
	void actualizarClienteCambiaDatos() {
		Cliente creado = clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"Empresa S.A.", "20123456789", "a@b.com", null, null, 1L));

		Cliente actualizado = clienteService.actualizarCliente(new GestionarCliente.ActualizarClienteCommand(
				creado.getId(), "Empresa Renombrada S.A.", "nuevo@b.com", "555-9999", "Calle 2", 1L));

		assertEquals("Empresa Renombrada S.A.", actualizado.getRazonSocial());
		assertEquals("nuevo@b.com", actualizado.getEmail());
		assertEquals("20123456789", actualizado.getCuit());
	}

	@Test
	void desactivarClientePersiste() {
		Cliente creado = clienteService.crearCliente(new GestionarCliente.CrearClienteCommand(
				"Empresa S.A.", "20123456789", null, null, null, 1L));
		clienteService.desactivarCliente(creado.getId());
		Cliente recargado = clienteRepository.findById(creado.getId()).orElseThrow();
		assertFalse(recargado.isActivo());
	}

	private static class FakeClienteRepository implements ClienteRepository {

		private final Map<Long, Cliente> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Cliente save(Cliente cliente) {
			if (cliente.getId() == null) {
				cliente.setId(secuencia.getAndIncrement());
			}
			datos.put(cliente.getId(), cliente);
			return cliente;
		}

		@Override
		public Optional<Cliente> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Cliente> findByCuit(String cuit) {
			return datos.values().stream().filter(c -> c.getCuit().equals(cuit)).findFirst();
		}

		@Override
		public List<Cliente> findAll() {
			return new ArrayList<>(datos.values());
		}

		@Override
		public List<Cliente> findByZonaId(Long zonaId) {
			return datos.values().stream()
					.filter(c -> c.getZona() != null && c.getZona().getId().equals(zonaId))
					.toList();
		}

		@Override
		public PageResponse<Cliente> buscar(String q, Long zonaId, int page, int size) {
			List<Cliente> todos = datos.values().stream()
					.filter(c -> zonaId == null || (c.getZona() != null && c.getZona().getId().equals(zonaId)))
					.filter(c -> q == null || q.isBlank()
							|| c.getRazonSocial().toLowerCase().contains(q.toLowerCase())
							|| c.getCuit().toLowerCase().contains(q.toLowerCase()))
					.toList();
			int total = todos.size();
			int from = Math.min(page * size, total);
			int to = Math.min(from + size, total);
			int totalPages = size == 0 ? 0 : (total + size - 1) / size;
			return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
		}
	}

	private static class FakeZonaRepository implements ZonaRepository {

		private final Map<Long, Zona> datos = new HashMap<>();

		@Override
		public Zona save(Zona zona) {
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
