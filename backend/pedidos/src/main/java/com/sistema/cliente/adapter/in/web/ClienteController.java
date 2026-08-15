package com.sistema.cliente.adapter.in.web;

import com.sistema.cliente.adapter.in.web.dto.ActualizarClienteRequest;
import com.sistema.cliente.adapter.in.web.dto.ClienteRequest;
import com.sistema.cliente.adapter.in.web.dto.ClienteResponse;
import com.sistema.cliente.model.Cliente;
import com.sistema.cliente.port.in.ConsultarCliente;
import com.sistema.cliente.port.in.GestionarCliente;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.common.util.CsvWriter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes")
public class ClienteController {

	private final GestionarCliente gestionarCliente;
	private final ConsultarCliente consultarCliente;

	public ClienteController(GestionarCliente gestionarCliente, ConsultarCliente consultarCliente) {
		this.gestionarCliente = gestionarCliente;
		this.consultarCliente = consultarCliente;
	}

	@PostMapping
	public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
		Cliente cliente = gestionarCliente.crearCliente(new GestionarCliente.CrearClienteCommand(
				request.razonSocial(), request.cuit(), request.email(), request.telefono(),
				request.domicilio(), request.zonaId()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(cliente));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarClienteRequest request) {
		Cliente cliente = gestionarCliente.actualizarCliente(new GestionarCliente.ActualizarClienteCommand(
				id, request.razonSocial(), request.email(), request.telefono(), request.domicilio(), request.zonaId()));
		return ResponseEntity.ok(ClienteResponse.from(cliente));
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarCliente.desactivarCliente(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarCliente.reactivarCliente(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/exportar.csv")
	public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) Long zonaId) {
		List<Cliente> clientes = zonaId == null ? consultarCliente.listarTodos()
				: consultarCliente.listarPorZona(zonaId);
		List<String> headers = List.of("id", "razonSocial", "cuit", "email", "telefono", "zonaNombre", "activo");
		List<List<String>> filas = new ArrayList<>();
		for (Cliente cliente : clientes) {
			filas.add(java.util.Arrays.asList(String.valueOf(cliente.getId()), cliente.getRazonSocial(),
					cliente.getCuit(), cliente.getEmail(), cliente.getTelefono(),
					cliente.getZona() == null ? "" : cliente.getZona().getNombre(),
					String.valueOf(cliente.isActivo())));
		}
		String csv = CsvWriter.escribir(headers, filas);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
		responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clientes.csv\"");
		return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
		Cliente cliente = consultarCliente.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + id));
		return ResponseEntity.ok(ClienteResponse.from(cliente));
	}

	@GetMapping("/buscar/cuit/{cuit}")
	public ResponseEntity<ClienteResponse> buscarPorCuit(@PathVariable String cuit) {
		Cliente cliente = consultarCliente.buscarPorCuit(cuit)
				.orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + cuit));
		return ResponseEntity.ok(ClienteResponse.from(cliente));
	}

	@GetMapping
	public PageResponse<ClienteResponse> listar(@RequestParam(required = false) String q,
			@RequestParam(required = false) Long zonaId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		PageResponse<Cliente> pagina = consultarCliente.listarPaginado(q, zonaId, page, size);
		return new PageResponse<>(pagina.content().stream().map(ClienteResponse::from).toList(),
				pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
	}
}
