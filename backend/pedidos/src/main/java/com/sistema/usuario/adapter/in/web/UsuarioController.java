package com.sistema.usuario.adapter.in.web;

import com.sistema.usuario.adapter.in.web.dto.AsignarRolesRequest;
import com.sistema.usuario.adapter.in.web.dto.CambiarPasswordRequest;
import com.sistema.usuario.adapter.in.web.dto.UsuarioRequest;
import com.sistema.usuario.adapter.in.web.dto.UsuarioResponse;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import com.sistema.usuario.port.in.GestionarUsuario;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {

	private final GestionarUsuario gestionarUsuario;
	private final ConsultarUsuario consultarUsuario;

	public UsuarioController(GestionarUsuario gestionarUsuario, ConsultarUsuario consultarUsuario) {
		this.gestionarUsuario = gestionarUsuario;
		this.consultarUsuario = consultarUsuario;
	}

	@PostMapping
	public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
		Usuario usuario = gestionarUsuario.crearUsuario(new GestionarUsuario.CrearUsuarioCommand(
				request.nombre(), request.email(), request.password(), request.roles()));
		return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
	}

	@PostMapping("/{id}/roles")
	public ResponseEntity<UsuarioResponse> asignarRoles(@PathVariable Long id, @Valid @RequestBody AsignarRolesRequest request) {
		gestionarUsuario.asignarRoles(new GestionarUsuario.AsignarRolesCommand(id, request.roles()));
		return consultarUsuario.buscarPorId(id)
				.map(u -> ResponseEntity.ok(UsuarioResponse.from(u)))
				.orElse(ResponseEntity.notFound().build());
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarUsuario.desactivarUsuario(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarUsuario.reactivarUsuario(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/password")
	public ResponseEntity<Void> cambiarPassword(@PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest request) {
		gestionarUsuario.cambiarPassword(new GestionarUsuario.CambiarPasswordCommand(id, request.password(), obtenerActorActual()));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
		Usuario usuario = consultarUsuario.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + id));
		return ResponseEntity.ok(UsuarioResponse.from(usuario));
	}

	@GetMapping
	public List<UsuarioResponse> listar() {
		return consultarUsuario.listarTodos().stream().map(UsuarioResponse::from).toList();
	}

	private com.sistema.usuario.model.Usuario obtenerActorActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof com.sistema.usuario.model.Usuario usuario) {
			return usuario;
		}
		throw new BusinessException("AUTH_INVALIDO", "No autenticado");
	}
}
