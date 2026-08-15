package com.sistema.cobranza.adapter.in.web;

import com.sistema.cobranza.adapter.in.web.dto.RemitoResponse;
import com.sistema.cobranza.model.Remito;
import com.sistema.cobranza.port.in.ConsultarRemito;
import com.sistema.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/remitos")
@Tag(name = "Remitos")
public class RemitoController {

	private final ConsultarRemito consultarRemito;

	public RemitoController(ConsultarRemito consultarRemito) {
		this.consultarRemito = consultarRemito;
	}

	@GetMapping
	public ResponseEntity<List<RemitoResponse>> listar(@RequestParam(required = false) Long pedidoId,
			@RequestParam(required = false) Long clienteId) {
		List<Remito> remitos = consultarRemito.listar(pedidoId, clienteId);
		return ResponseEntity.ok(remitos.stream().map(RemitoResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<RemitoResponse> buscarPorId(@PathVariable Long id) {
		Remito remito = consultarRemito.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Remito no encontrado: " + id));
		return ResponseEntity.ok(RemitoResponse.from(remito));
	}
}
