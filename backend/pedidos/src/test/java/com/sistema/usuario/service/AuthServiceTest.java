package com.sistema.usuario.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.security.JwtService;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

	private static final String SECRET = "clave-de-prueba-suficientemente-larga-para-hs256-2026";

	private AuthService authService;
	private Usuario usuario;

	@BeforeEach
	void setUp() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		usuario = new Usuario("Admin", "admin@test.com", encoder.encode("admin123"), Set.of(Rol.ADMINISTRATIVO));
		usuario.setId(1L);
		JwtService jwtService = new JwtService(SECRET, 3600000L, new MockEnvironment());
		ConsultarUsuario consultar = new ConsultarUsuario() {
			@Override
			public Optional<Usuario> buscarPorId(Long id) {
				return usuario.getId().equals(id) ? Optional.of(usuario) : Optional.empty();
			}

			@Override
			public Optional<Usuario> buscarPorEmail(String email) {
				return usuario.getEmail().equals(email) ? Optional.of(usuario) : Optional.empty();
			}

			@Override
			public List<Usuario> listarTodos() {
				return List.of(usuario);
			}

			@Override
			public com.sistema.common.model.PageResponse<Usuario> listarPaginado(String q, int page, int size) {
				return new com.sistema.common.model.PageResponse<>(List.of(), page, size, 0, 0);
			}
		};
		authService = new AuthService(consultar, encoder, jwtService);
	}

	@Test
	void loginConCredencialesValidasDevuelveToken() {
		AuthService.LoginResultado resultado = authService.login("admin@test.com", "admin123");

		assertNotNull(resultado.token());
		assertEquals(usuario.getId(), resultado.usuario().getId());
		assertTrue(resultado.token().split("\\.").length == 3);
	}

	@Test
	void loginConPasswordIncorrectaLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> authService.login("admin@test.com", "incorrecta"));
	}

	@Test
	void loginUsuarioInexistenteLanzaBusinessException() {
		assertThrows(BusinessException.class, () -> authService.login("nadie@test.com", "admin123"));
	}

	@Test
	void loginUsuarioInactivoLanzaBusinessException() {
		usuario.desactivar();
		assertThrows(BusinessException.class, () -> authService.login("admin@test.com", "admin123"));
	}
}
