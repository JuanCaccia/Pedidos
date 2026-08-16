package com.sistema.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

	private static final String SECRET_DEFAULT_INSEGURO = "cambiar-en-produccion-esta-clave-secreta-jwt-2026";
	private static final String SECRET_CUSTOMO = "clave-custom-de-prueba-suficientemente-larga-para-hs256-2026";

	private MockEnvironment entorno(String... perfiles) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(perfiles);
		return environment;
	}

	@Test
	void prodConDefaultInseguroLanzaIllegalStateException() {
		assertThrows(IllegalStateException.class,
				() -> new JwtService(SECRET_DEFAULT_INSEGURO, 3600000L, entorno("prod")));
	}

	@Test
	void prodConSecretVacioLanzaIllegalStateException() {
		assertThrows(IllegalStateException.class, () -> new JwtService("", 3600000L, entorno("prod")));
		assertThrows(IllegalStateException.class, () -> new JwtService("   ", 3600000L, entorno("prod")));
		assertThrows(IllegalStateException.class, () -> new JwtService(null, 3600000L, entorno("prod")));
	}

	@Test
	void devConDefaultEsAceptado() {
		assertDoesNotThrow(() -> new JwtService(SECRET_DEFAULT_INSEGURO, 3600000L, entorno("dev")));
	}

	@Test
	void prodConSecretCustomEsAceptado() {
		assertDoesNotThrow(() -> new JwtService(SECRET_CUSTOMO, 3600000L, entorno("prod")));
	}
}
