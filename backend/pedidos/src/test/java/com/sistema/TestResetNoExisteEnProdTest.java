package com.sistema;

import com.sistema.config.TestResetController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = "app.jwt.secret=clave-custom-de-prueba-suficientemente-larga-para-hs256-2026")
class TestResetNoExisteEnProdTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testResetControllerNoRegistradoEnPerfilProd() {
		assertTrue(applicationContext.getBeansOfType(TestResetController.class).isEmpty(),
				"El endpoint /test/reset NO debe existir en perfil prod");
	}
}
