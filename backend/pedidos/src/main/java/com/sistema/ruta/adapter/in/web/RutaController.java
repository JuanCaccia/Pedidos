package com.sistema.ruta.adapter.in.web;

import com.sistema.ruta.adapter.in.web.dto.AsignarPedidosRequest;
import com.sistema.ruta.adapter.in.web.dto.RutaRequest;
import com.sistema.ruta.adapter.in.web.dto.RutaResponse;
import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.ruta.port.in.GestionarRuta;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/rutas")
@Tag(name = "Rutas")
public class RutaController {

	private final GestionarRuta gestionarRuta;
	private final ConsultarRuta consultarRuta;

	public RutaController(GestionarRuta gestionarRuta, ConsultarRuta consultarRuta) {
		this.gestionarRuta = gestionarRuta;
		this.consultarRuta = consultarRuta;
	}

	@PostMapping
	public ResponseEntity<RutaResponse> crear(@RequestBody RutaRequest request) {
		Ruta ruta = gestionarRuta.crearRuta(new GestionarRuta.CrearRutaCommand(request.zonaId(),
				request.repartidorId(), request.fechaJornada(), request.pedidoIds()));
		return ResponseEntity.status(HttpStatus.CREATED).body(RutaResponse.from(ruta));
	}

	@PostMapping("/{id}/pedidos")
	public ResponseEntity<RutaResponse> asignarPedidos(@PathVariable Long id, @RequestBody AsignarPedidosRequest request) {
		Ruta ruta = gestionarRuta.asignarPedidos(id, request.pedidoIds());
		return ResponseEntity.ok(RutaResponse.from(ruta));
	}

	@PostMapping("/{id}/iniciar")
	public ResponseEntity<RutaResponse> iniciarJornada(@PathVariable Long id) {
		Ruta ruta = gestionarRuta.iniciarJornada(id);
		return ResponseEntity.ok(RutaResponse.from(ruta));
	}

	@PostMapping("/{id}/cerrar")
	public ResponseEntity<RutaResponse> cerrarJornada(@PathVariable Long id) {
		Ruta ruta = gestionarRuta.cerrarJornada(id);
		return ResponseEntity.ok(RutaResponse.from(ruta));
	}

	@GetMapping
	public ResponseEntity<List<RutaResponse>> listar(@RequestParam(required = false) LocalDate fecha,
			@RequestParam(required = false) Long repartidorId, @RequestParam(required = false) String estado) {
		List<Ruta> rutas;
		if (fecha != null) {
			rutas = consultarRuta.listarPorFecha(fecha);
		} else if (repartidorId != null) {
			rutas = consultarRuta.listarPorRepartidor(repartidorId);
		} else if (estado != null) {
			try {
				rutas = consultarRuta.listarPorEstado(EstadoRuta.valueOf(estado));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().build();
			}
		} else {
			rutas = consultarRuta.listarTodos();
		}
		return ResponseEntity.ok(rutas.stream().map(RutaResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<RutaResponse> buscarPorId(@PathVariable Long id) {
		return consultarRuta.buscarPorId(id)
				.map(RutaResponse::from)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
