package com.sistema.usuario.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.GestionarUsuario;
import com.sistema.usuario.port.out.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioServiceTest {

	private UsuarioService usuarioService;
	private FakeUsuarioRepository repository;

	@BeforeEach
	void setUp() {
		repository = new FakeUsuarioRepository();
		usuarioService = new UsuarioService(repository, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
	}

	@Test
	void crearUsuarioConRolesAsignaYPersiste() {
		Usuario usuario = usuarioService.crearUsuario(new GestionarUsuario.CrearUsuarioCommand(
				"Juan", "juan@test.com", "123456", Set.of(Rol.VENDEDOR, Rol.REPARTIDOR)));

		assertEquals("juan@test.com", usuario.getEmail());
		assertTrue(usuario.tieneRol(Rol.VENDEDOR));
		assertTrue(usuario.tieneRol(Rol.REPARTIDOR));
		assertTrue(usuario.isActivo());
		assertEquals(1, repository.findAll().size());
	}

	@Test
	void crearUsuarioConEmailDuplicadoLanzaBusinessException() {
		usuarioService.crearUsuario(new GestionarUsuario.CrearUsuarioCommand(
				"Otro", "dup@test.com", "123456", Set.of(Rol.VENDEDOR)));

		assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(
				new GestionarUsuario.CrearUsuarioCommand("Nuevo", "DUP@test.com", "123456", Set.of(Rol.VENDEDOR))));
	}

	@Test
	void crearUsuarioSinRolesLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(
				new GestionarUsuario.CrearUsuarioCommand("Sin Roles", "sin@test.com", "123456", Set.of())));
	}

	@Test
	void asignarRolesYDesactivarPersisten() {
		Usuario usuario = usuarioService.crearUsuario(new GestionarUsuario.CrearUsuarioCommand(
				"Ana", "ana@test.com", "123456", Set.of(Rol.VENDEDOR)));

		usuarioService.asignarRoles(new GestionarUsuario.AsignarRolesCommand(usuario.getId(), Set.of(Rol.ENCARGADO_DEPOSITO)));
		usuarioService.desactivarUsuario(usuario.getId());

		Usuario recargado = repository.findById(usuario.getId()).orElseThrow();
		assertTrue(recargado.tieneRol(Rol.ENCARGADO_DEPOSITO));
		assertFalse(recargado.isActivo());
	}

	@Test
	void desactivarUsuarioInexistenteLanzaNotFoundException() {
		assertThrows(NotFoundException.class, () -> usuarioService.desactivarUsuario(999L));
	}

	private static class FakeUsuarioRepository implements UsuarioRepository {

		private final Map<Long, Usuario> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Usuario save(Usuario usuario) {
			if (usuario.getId() == null) {
				usuario.setId(secuencia.getAndIncrement());
			}
			datos.put(usuario.getId(), usuario);
			return usuario;
		}

		@Override
		public Optional<Usuario> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Usuario> findByEmail(String email) {
			return datos.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
		}

		@Override
		public List<Usuario> findAll() {
			return new ArrayList<>(datos.values());
		}
	}
}
