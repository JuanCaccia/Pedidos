package com.sistema.sustitucion.adapter.in.web;

import com.sistema.common.exception.BusinessException;
import com.sistema.sustitucion.adapter.in.web.dto.SustituirRequest;
import com.sistema.sustitucion.adapter.in.web.dto.SustitucionResponse;
import com.sistema.sustitucion.model.Sustitucion;
import com.sistema.sustitucion.port.in.RegistrarSustitucion;
import com.sistema.sustitucion.port.in.RegistrarSustitucion.SustituirCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sustituciones")
@Tag(name = "Sustituciones")
public class SustitucionController {

	private final RegistrarSustitucion registrarSustitucion;

	public SustitucionController(RegistrarSustitucion registrarSustitucion) {
		this.registrarSustitucion = registrarSustitucion;
	}

	@PostMapping
	@Operation(summary = "Registra una sustitución en destino y ajusta el cierre del camión")
	public ResponseEntity<SustitucionResponse> sustituir(@Valid @RequestBody SustituirRequest request) {
		Sustitucion sustitucion = registrarSustitucion.sustituir(new SustituirCommand(request.pedidoId(),
				request.itemOriginalId(), request.itemSustitutoId(), request.cantidad(), request.observaciones(),
				obtenerActorActual()));
		return ResponseEntity.status(HttpStatus.CREATED).body(SustitucionResponse.from(sustitucion));
	}

	private com.sistema.usuario.model.Usuario obtenerActorActual() {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof com.sistema.usuario.model.Usuario usuario) {
			return usuario;
		}
		throw new BusinessException("AUTH_INVALIDO", "No autenticado");
	}
}
