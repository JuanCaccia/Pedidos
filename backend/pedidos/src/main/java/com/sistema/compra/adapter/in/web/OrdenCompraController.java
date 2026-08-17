package com.sistema.compra.adapter.in.web;

import com.sistema.common.exception.NotFoundException;
import com.sistema.compra.adapter.in.web.dto.CrearOrdenCompraRequest;
import com.sistema.compra.adapter.in.web.dto.LineaOrdenRequest;
import com.sistema.compra.adapter.in.web.dto.OrdenCompraResponse;
import com.sistema.compra.adapter.in.web.dto.RecepcionLineaRequest;
import com.sistema.compra.adapter.in.web.dto.RecepcionRequest;
import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.port.in.ConsultarOrdenCompra;
import com.sistema.compra.port.in.GestionarOrdenCompra;
import com.sistema.compra.port.in.ImportarRecepcionCsv;
import com.sistema.stock.adapter.in.web.dto.ImportacionCsvResponse;
import com.sistema.stock.model.Lote;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/ordenes-compra")
@Tag(name = "Compras")
public class OrdenCompraController {

	private final GestionarOrdenCompra gestionarOrdenCompra;
	private final ConsultarOrdenCompra consultarOrdenCompra;
	private final ImportarRecepcionCsv importarRecepcionCsv;

	public OrdenCompraController(GestionarOrdenCompra gestionarOrdenCompra, ConsultarOrdenCompra consultarOrdenCompra,
			ImportarRecepcionCsv importarRecepcionCsv) {
		this.gestionarOrdenCompra = gestionarOrdenCompra;
		this.consultarOrdenCompra = consultarOrdenCompra;
		this.importarRecepcionCsv = importarRecepcionCsv;
	}

	@PostMapping
	public ResponseEntity<OrdenCompraResponse> crear(@Valid @RequestBody CrearOrdenCompraRequest request) {
		List<GestionarOrdenCompra.LineaOrdenCommand> lineas = request.lineas().stream()
				.map(l -> new GestionarOrdenCompra.LineaOrdenCommand(l.itemId(), l.cantidad()))
				.toList();
		OrdenCompra orden = gestionarOrdenCompra.crearOrdenCompra(new GestionarOrdenCompra.CrearOrdenCompraCommand(
				request.proveedorId(), request.observaciones(), lineas));
		return ResponseEntity.status(HttpStatus.CREATED).body(OrdenCompraResponse.from(orden));
	}

	@PostMapping("/{id}/recepciones")
	public ResponseEntity<OrdenCompraResponse> registrarRecepcion(@PathVariable Long id,
			@Valid @RequestBody RecepcionRequest request) {
		List<GestionarOrdenCompra.RecepcionLineaCommand> lineas = request.lineas().stream()
				.map(rl -> new GestionarOrdenCompra.RecepcionLineaCommand(rl.lineaId(), rl.cantidadRecibida(), rl.precioUnitario()))
				.toList();
		OrdenCompra orden = gestionarOrdenCompra.registrarRecepcion(new GestionarOrdenCompra.RecepcionCommand(id, lineas));
		return ResponseEntity.ok(OrdenCompraResponse.from(orden));
	}

	@PostMapping("/{id}/recepciones/csv")
	public ResponseEntity<ImportacionCsvResponse> importarRecepcionCsv(@PathVariable Long id,
			@RequestPart("file") MultipartFile file) throws IOException {
		String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
		List<Lote> lotes = importarRecepcionCsv.importar(id, csv);
		return ResponseEntity.ok(ImportacionCsvResponse.de(lotes));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<Void> cancelar(@PathVariable Long id) {
		gestionarOrdenCompra.cancelarOrdenCompra(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<OrdenCompraResponse>> listar(@RequestParam(required = false) String estado,
			@RequestParam(required = false) Long proveedorId) {
		List<OrdenCompra> ordenes;
		if (estado != null) {
			ordenes = consultarOrdenCompra.listarPorEstado(EstadoOrdenCompra.valueOf(estado));
		} else if (proveedorId != null) {
			ordenes = consultarOrdenCompra.listarPorProveedor(proveedorId);
		} else {
			ordenes = consultarOrdenCompra.listarTodas();
		}
		return ResponseEntity.ok(ordenes.stream().map(OrdenCompraResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrdenCompraResponse> buscarPorId(@PathVariable Long id) {
		OrdenCompra orden = consultarOrdenCompra.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Orden de compra no encontrada: " + id));
		return ResponseEntity.ok(OrdenCompraResponse.from(orden));
	}
}
