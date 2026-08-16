package com.sistema.categoria.adapter.in.web;

import com.sistema.categoria.adapter.in.web.dto.CategoriaRequest;
import com.sistema.categoria.adapter.in.web.dto.CategoriaResponse;
import com.sistema.categoria.model.Categoria;
import com.sistema.categoria.port.in.ConsultarCategoria;
import com.sistema.categoria.port.in.GestionarCategoria;
import com.sistema.common.exception.NotFoundException;
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

import java.util.List;

@RestController
@RequestMapping("/categorias")
@Tag(name = "Categorías")
public class CategoriaController {

	private final GestionarCategoria gestionarCategoria;
	private final ConsultarCategoria consultarCategoria;

	public CategoriaController(GestionarCategoria gestionarCategoria, ConsultarCategoria consultarCategoria) {
		this.gestionarCategoria = gestionarCategoria;
		this.consultarCategoria = consultarCategoria;
	}

	@GetMapping
	public List<CategoriaResponse> listar(@RequestParam(required = false) Boolean todas) {
		List<Categoria> categorias = Boolean.TRUE.equals(todas)
				? consultarCategoria.listarTodas()
				: consultarCategoria.listarActivas();
		return categorias.stream().map(CategoriaResponse::from).toList();
	}

	@PostMapping
	public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
		Categoria categoria = gestionarCategoria.crear(new GestionarCategoria.CrearCategoriaCommand(request.nombre()));
		return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.from(categoria));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
		Categoria categoria = gestionarCategoria.actualizar(new GestionarCategoria.ActualizarCategoriaCommand(id, request.nombre()));
		return ResponseEntity.ok(CategoriaResponse.from(categoria));
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarCategoria.desactivar(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarCategoria.reactivar(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
		Categoria categoria = consultarCategoria.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Categoría no encontrada: " + id));
		return ResponseEntity.ok(CategoriaResponse.from(categoria));
	}
}
