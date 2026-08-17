package com.sistema.cliente.adapter.in.web;

import com.sistema.cliente.adapter.in.web.dto.ZonaRequest;
import com.sistema.cliente.adapter.in.web.dto.ZonaResponse;
import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.Zona;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/zonas")
@Tag(name = "Zonas")
public class ZonaController {

	private final GestionarZona gestionarZona;

	public ZonaController(GestionarZona gestionarZona) {
		this.gestionarZona = gestionarZona;
	}

	@PostMapping
	public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody ZonaRequest request) {
		Zona zona = gestionarZona.crearZona(new GestionarZona.CrearZonaCommand(request.nombre()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ZonaResponse.from(zona));
	}

	@GetMapping
	public List<ZonaResponse> listar() {
		return gestionarZona.listarTodas().stream().map(ZonaResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ZonaResponse> buscarPorId(@PathVariable Long id) {
		Zona zona = gestionarZona.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Zona no encontrada: " + id));
		return ResponseEntity.ok(ZonaResponse.from(zona));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ZonaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ZonaRequest request) {
		Zona zona = gestionarZona.actualizarZona(new GestionarZona.ActualizarZonaCommand(id, request.nombre()));
		return ResponseEntity.ok(ZonaResponse.from(zona));
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarZona.desactivarZona(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarZona.reactivarZona(id);
		return ResponseEntity.noContent().build();
	}
}
