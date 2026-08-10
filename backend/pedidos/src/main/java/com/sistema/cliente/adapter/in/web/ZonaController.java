package com.sistema.cliente.adapter.in.web;

import com.sistema.cliente.adapter.in.web.dto.ZonaRequest;
import com.sistema.cliente.adapter.in.web.dto.ZonaResponse;
import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.common.model.Zona;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
