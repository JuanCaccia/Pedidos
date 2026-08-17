package com.sistema.cobranza.adapter.in.web;

import com.sistema.cobranza.adapter.in.web.dto.CobranzaRequest;
import com.sistema.cobranza.adapter.in.web.dto.CobranzaResponse;
import com.sistema.cobranza.model.Cobranza;
import com.sistema.cobranza.port.in.ConsultarCobranza;
import com.sistema.cobranza.port.in.ConsultarCobranza.EstadoCuenta;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.cobranza.port.in.RegistrarCobranza.RegistrarCobranzaCommand;
import com.sistema.common.exception.BusinessException;
import com.sistema.usuario.model.Usuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/cobranzas")
@Tag(name = "Cobranzas")
public class CobranzaController {

	private final RegistrarCobranza registrarCobranza;
	private final ConsultarCobranza consultarCobranza;

	public CobranzaController(RegistrarCobranza registrarCobranza, ConsultarCobranza consultarCobranza) {
		this.registrarCobranza = registrarCobranza;
		this.consultarCobranza = consultarCobranza;
	}

	@PostMapping
	public ResponseEntity<CobranzaResponse> registrar(@Valid @RequestBody CobranzaRequest request) {
		Cobranza cobranza = registrarCobranza.registrar(new RegistrarCobranzaCommand(request.clienteId(),
				request.pedidoId(), request.monto(), request.formaPago(), request.observaciones(), obtenerActorActual()));
		return ResponseEntity.status(HttpStatus.CREATED).body(CobranzaResponse.from(cobranza));
	}

	@GetMapping
	public ResponseEntity<List<CobranzaResponse>> listar(
			@RequestParam(required = false) Long clienteId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		List<Cobranza> cobranzas = consultarCobranza.listar(clienteId, desde, hasta);
		return ResponseEntity.ok(cobranzas.stream().map(CobranzaResponse::from).toList());
	}

	@GetMapping("/clientes/{id}/cuenta")
	public ResponseEntity<EstadoCuenta> estadoCuenta(@PathVariable Long id) {
		return ResponseEntity.ok(consultarCobranza.estadoCuenta(id));
	}

	private Usuario obtenerActorActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
			return usuario;
		}
		throw new BusinessException("AUTH_INVALIDO", "No autenticado");
	}
}
