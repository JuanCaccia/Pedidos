package com.sistema.compra.adapter.in.web;

import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import com.sistema.compra.adapter.in.web.dto.ActualizarProveedorRequest;
import com.sistema.compra.adapter.in.web.dto.ProveedorRequest;
import com.sistema.compra.adapter.in.web.dto.ProveedorResponse;
import com.sistema.compra.model.Proveedor;
import com.sistema.compra.port.in.ConsultarProveedor;
import com.sistema.compra.port.in.GestionarProveedor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/proveedores")
@Tag(name = "Proveedores")
public class ProveedorController {

	private final GestionarProveedor gestionarProveedor;
	private final ConsultarProveedor consultarProveedor;

	public ProveedorController(GestionarProveedor gestionarProveedor, ConsultarProveedor consultarProveedor) {
		this.gestionarProveedor = gestionarProveedor;
		this.consultarProveedor = consultarProveedor;
	}

	@PostMapping
	public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
		Proveedor proveedor = gestionarProveedor.crearProveedor(new GestionarProveedor.CrearProveedorCommand(
				request.razonSocial(), request.cuit(), request.email(), request.telefono()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponse.from(proveedor));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProveedorRequest request) {
		Proveedor proveedor = gestionarProveedor.actualizarProveedor(new GestionarProveedor.ActualizarProveedorCommand(
				id, request.razonSocial(), request.email(), request.telefono()));
		return ResponseEntity.ok(ProveedorResponse.from(proveedor));
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarProveedor.desactivarProveedor(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarProveedor.reactivarProveedor(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public PageResponse<ProveedorResponse> listar(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		PageResponse<Proveedor> pagina = consultarProveedor.listarPaginado(q, page, size);
		return PageMapper.of(pagina.content(), pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages(), ProveedorResponse::from);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProveedorResponse> buscarPorId(@PathVariable Long id) {
		Proveedor proveedor = consultarProveedor.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Proveedor no encontrado: " + id));
		return ResponseEntity.ok(ProveedorResponse.from(proveedor));
	}
}
