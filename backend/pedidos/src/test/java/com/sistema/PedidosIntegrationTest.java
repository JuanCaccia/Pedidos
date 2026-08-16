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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
		Sesion sesion = loginSesion();
		String token = sesion.token;
		Integer clienteId = obtenerClienteId(token, "Cliente Demo");
		Integer vendedorId = sesion.usuarioId;
		Integer itemId = obtenerItemId(token, "HAR");

		String pedido = mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clienteId\":" + clienteId + ",\"vendedorId\":" + vendedorId + ",\"items\":[{\"itemId\":" + itemId + ",\"cantidad\":2.000,\"precioUnitario\":3.50}]}"))
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
		Sesion sesion = loginSesion();
		String token = sesion.token;
		Integer vendedorId = sesion.usuarioId;
		Integer itemId = obtenerItemId(token, "HAR");

		mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"vendedorId\":" + vendedorId + ",\"items\":[{\"itemId\":" + itemId + ",\"cantidad\":1,\"precioUnitario\":1}]}"))
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
	void pedidoConItemInactivoRechazado() throws Exception {
		Sesion sesion = loginSesion();
		String token = sesion.token;
		Integer clienteId = obtenerClienteId(token, "Cliente Demo");
		Integer vendedorId = sesion.usuarioId;

		String creado = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"INACT-IT\",\"nombre\":\"Item a desactivar\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(creado, "$.id");

		mockMvc.perform(patch("/items/" + itemId + "/desactivar").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clienteId\":" + clienteId + ",\"vendedorId\":" + vendedorId + ",\"items\":[{\"itemId\":" + itemId
								+ ",\"cantidad\":1,\"precioUnitario\":1}]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ITEM_INACTIVO"));
	}

	@Test
	void itemInactivoNoApareceEnBusquedaDeActivos() throws Exception {
		String token = login();

		String creado = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"BUSQ-ACT\",\"nombre\":\"Busqueda activos\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(creado, "$.id");

		mockMvc.perform(patch("/items/" + itemId + "/desactivar").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		// Combobox (solo activos) no lo devuelve.
		mockMvc.perform(get("/items").param("q", "BUSQ-ACT").param("activos", "true")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		// ABMC maestro (todos) lo sigue mostrando para poder reactivarlo.
		mockMvc.perform(get("/items").param("q", "BUSQ-ACT")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void encargadoDepositoPuedeVerStockPeroNoRestoDeReportes() throws Exception {
		String token = login();

		// ENCARGADO_DEPOSITO (admin tiene ese rol) puede leer stock por item.
		mockMvc.perform(get("/reportes/stock").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/reportes/stock/exportar.csv").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		// El resto de reportes sigue restringido a ADMINISTRATIVO.
		String repartidorLogin = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"repartidor@pedidos.com\",\"password\":\"repartidor123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String repartidorToken = JsonPath.read(repartidorLogin, "$.token");

		mockMvc.perform(get("/reportes/ventas").header("Authorization", "Bearer " + repartidorToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/reportes/stock").header("Authorization", "Bearer " + repartidorToken))
				.andExpect(status().isForbidden());
	}

	private String login() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(login, "$.token");
	}

	private record Sesion(String token, Integer usuarioId) {
	}

	private Sesion loginSesion() throws Exception {
		String login = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"admin@pedidos.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return new Sesion(JsonPath.read(login, "$.token"), JsonPath.read(login, "$.usuarioId"));
	}

	// Resuelve ids en runtime (igual que la suite E2E) para no depender de que la
	// BD esté fresca con ids 1..N.
	private Integer obtenerClienteId(String token, String q) throws Exception {
		String body = mockMvc.perform(get("/clientes").param("q", q).param("size", "1")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.content[0].id");
	}

	private Integer obtenerItemId(String token, String q) throws Exception {
		String body = mockMvc.perform(get("/items").param("q", q).param("size", "1")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.content[0].id");
	}

	@Test
	void descartarLoteConSaldoMarcaDescartadoYReduceStock() throws Exception {
		String token = login();
		// Creo un item + ingreso para obtener un lote con saldo.
		String itemJson = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"DESC-INT\",\"nombre\":\"Descartable\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(itemJson, "$.id");

		String ingreso = mockMvc.perform(post("/stock/ingresos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"itemId\":" + itemId + ",\"cantidad\":100.000,\"motivo\":\"test\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer loteId = JsonPath.read(ingreso, "$.loteId");

		mockMvc.perform(post("/stock/lotes/" + loteId + "/descartar")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DESCARTADO"))
				.andExpect(jsonPath("$.disponible").value(0.0));
	}

	@Test
	void descartarLoteRechazaDescartadoYForbidenRepartidor() throws Exception {
		String token = login();
		String itemJson = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"DESC-INT2\",\"nombre\":\"Descartable2\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(itemJson, "$.id");
		String ingreso = mockMvc.perform(post("/stock/ingresos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"itemId\":" + itemId + ",\"cantidad\":50.000,\"motivo\":\"test\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer loteId = JsonPath.read(ingreso, "$.loteId");

		mockMvc.perform(post("/stock/lotes/" + loteId + "/descartar")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(post("/stock/lotes/" + loteId + "/descartar")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());

		String repartidorLogin = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"repartidor@pedidos.com\",\"password\":\"repartidor123\"}"))
				.andReturn().getResponse().getContentAsString();
		String repartidorToken = JsonPath.read(repartidorLogin, "$.token");
		mockMvc.perform(post("/stock/lotes/" + loteId + "/descartar")
						.header("Authorization", "Bearer " + repartidorToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void loteVencidoSeReportaComoVencido() throws Exception {
		String token = login();
		String itemJson = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"VENC-INT\",\"nombre\":\"Vencible\",\"unidadMedida\":\"UN\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(itemJson, "$.id");

		String ingreso = mockMvc.perform(post("/stock/ingresos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"itemId\":" + itemId + ",\"cantidad\":30.000,\"fechaVencimiento\":\"2020-01-01\",\"motivo\":\"test\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		Integer loteId = JsonPath.read(ingreso, "$.loteId");

		mockMvc.perform(get("/stock/lotes").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + loteId + ")].estado", org.hamcrest.Matchers.contains("VENCIDO")));
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
		Sesion sesion = loginSesion();
		String token = sesion.token;
		Integer clienteId = obtenerClienteId(token, "Cliente Demo");
		Integer vendedorId = sesion.usuarioId;

		mockMvc.perform(post("/pedidos")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clienteId\":" + clienteId + ",\"vendedorId\":" + vendedorId + ",\"items\":[{\"itemId\":null,\"cantidad\":1,\"precioUnitario\":1}]}"))
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

	@Test
	void categoriaRequiereDepositoParaEscribir() throws Exception {
		// repartidor autenticado puede listar, pero no crear.
		String repartidorLogin = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"repartidor@pedidos.com\",\"password\":\"repartidor123\"}"))
				.andReturn().getResponse().getContentAsString();
		String repartidorToken = JsonPath.read(repartidorLogin, "$.token");

		mockMvc.perform(get("/categorias").header("Authorization", "Bearer " + repartidorToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/categorias")
						.header("Authorization", "Bearer " + repartidorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"No autorizada\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void crearCategoriaYItemConCategoria() throws Exception {
		String token = login();

		// Crear una categoría.
		String categoria = mockMvc.perform(post("/categorias")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Limpieza IT\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Limpieza IT"))
				.andReturn().getResponse().getContentAsString();
		Integer categoriaId = JsonPath.read(categoria, "$.id");

		// Crear un item asociado a la categoría.
		String item = mockMvc.perform(post("/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sku\":\"IT-CAT-1\",\"nombre\":\"Lavandina\",\"unidadMedida\":\"UN\",\"categoriaId\":" + categoriaId + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoriaId").value(categoriaId))
				.andExpect(jsonPath("$.categoriaNombre").value("Limpieza IT"))
				.andReturn().getResponse().getContentAsString();
		Integer itemId = JsonPath.read(item, "$.id");

		// Buscar por id devuelve el nombre de la categoría resuelto.
		mockMvc.perform(get("/items/" + itemId).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoriaId").value(categoriaId))
				.andExpect(jsonPath("$.categoriaNombre").value("Limpieza IT"));

		// Listar categorías (activas) incluye la creada.
		mockMvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Limpieza IT')]").exists());

		// Filtrar items por categoriaId.
		mockMvc.perform(get("/items").param("categoriaId", String.valueOf(categoriaId))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
	}
}
