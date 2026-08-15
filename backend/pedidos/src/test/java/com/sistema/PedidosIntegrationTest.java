package com.sistema;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class PedidosIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/usuarios"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginConTokenPermiteAcceso() throws Exception {
		String body = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn().getResponse().getContentAsString();

		String token = JsonPath.read(body, "$.token");

		mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").exists());
	}

	@Test
	void flujoCompletoPedidoConfirmarReserva() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		String pedido = mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clienteId\":1,\"vendedorId\":1,\"items\":[{\"itemId\":1,\"cantidad\":2.000,\"precioUnitario\":3.50}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_CONFIRMACION"))
				.andReturn().getResponse().getContentAsString();
		Integer pedidoId = JsonPath.read(pedido, "$.id");

		mockMvc.perform(post("/pedidos/" + pedidoId + "/confirmar")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE_PREPARACION"))
				.andExpect(jsonPath("$.items[0].cantidadReservada").value(2.0));
	}

	@Test
	void pedidoInvalidoDevuelve400ConFieldErrors() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"vendedorId\":1,\"items\":[{\"itemId\":1,\"cantidad\":1,\"precioUnitario\":1}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.clienteId").exists());
	}

	@Test
	void repartidorNoPuedeCrearItemsNiClientes() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"repartidor@pedidos.com\",\"password\":\"repartidor123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"R-X\",\"nombre\":\"Repartidor item\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/clientes")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"razonSocial\":\"Repartidor Cliente\",\"cuit\":\"30112233440\",\"email\":\"r@x.com\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void encargadoDepositoPuedeCrearItems() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"A-ADMIN\",\"nombre\":\"Admin item\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void recursoInexistenteDevuelve404() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(get("/nonexistent").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void metodoNoSoportadoDevuelve405() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(get("/stock/mermas").header("Authorization", "Bearer " + token))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void sustitucionConBodyVacioDevuelve400() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/sustituciones")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void pedidoConLineaSinItemIdDevuelve400() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clienteId\":1,\"vendedorId\":1,\"items\":[{\"itemId\":null,\"cantidad\":1,\"precioUnitario\":1}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors['items[0].itemId']").exists());
	}

	@Test
	void entregaConListaVaciaDevuelve400() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/pedidos/1/entregas")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"entregas\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void entregaConCantidadCeroDevuelve400() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(login, "$.token");

		mockMvc.perform(post("/pedidos/1/entregas")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"entregas\":[{\"pedidoItemId\":1,\"cantidadEntregada\":0}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors['entregas[0].cantidadEntregada']").exists());
	}

	@Test
	void actuatorHealthPublicoDevuelve200() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void actuatorInfoRequiereAdministrativo() throws Exception {
		// sin token -> 401
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized());

		// repartidor -> 403
		String repartidorLogin = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"repartidor@pedidos.com\",\"password\":\"repartidor123\"}"))
				.andReturn().getResponse().getContentAsString();
		String repartidorToken = JsonPath.read(repartidorLogin, "$.token");
		mockMvc.perform(get("/actuator/info").header("Authorization", "Bearer " + repartidorToken))
				.andExpect(status().isForbidden());

		// admin -> 200
		String adminLogin = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andReturn().getResponse().getContentAsString();
		String adminToken = JsonPath.read(adminLogin, "$.token");
		mockMvc.perform(get("/actuator/info").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
	}
}
