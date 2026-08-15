package com.sistema.notificacion.adapter.in.web;

import com.sistema.common.exception.BusinessException;
import com.sistema.notificacion.model.Notificacion;
import com.sistema.notificacion.port.in.ConsultarNotificacion;
import com.sistema.notificacion.port.in.GestionarNotificacion;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificaciones")
@Tag(name = "Notificaciones")
public class NotificacionController {

	private final ConsultarNotificacion consultarNotificacion;
	private final GestionarNotificacion gestionarNotificacion;

	public NotificacionController(ConsultarNotificacion consultarNotificacion, GestionarNotificacion gestionarNotificacion) {
		this.consultarNotificacion = consultarNotificacion;
		this.gestionarNotificacion = gestionarNotificacion;
	}

	@GetMapping
	public List<NotificacionResponse> listar(@RequestParam(defaultValue = "false") boolean soloNoLeidas) {
		return consultarNotificacion.listar(obtenerActorActual().getId(), soloNoLeidas).stream()
				.map(NotificacionResponse::from)
				.toList();
	}

	@GetMapping("/no-leidas")
	public Map<String, Long> contarNoLeidas() {
		return Map.of("cantidad", consultarNotificacion.contarNoLeidas(obtenerActorActual().getId()));
	}

	@PostMapping("/{id}/leer")
	public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
		gestionarNotificacion.marcarLeida(id, obtenerActorActual().getId());
		return ResponseEntity.noContent().build();
	}

	private com.sistema.usuario.model.Usuario obtenerActorActual() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof com.sistema.usuario.model.Usuario usuario) {
			return usuario;
		}
		throw new BusinessException("AUTH_INVALIDO", "No autenticado");
	}

	public record NotificacionResponse(Long id, String tipo, String mensaje, Long paraUsuarioId, Long pedidoId,
			boolean leida, LocalDateTime fecha) {

		public static NotificacionResponse from(Notificacion n) {
			return new NotificacionResponse(n.getId(), n.getTipo(), n.getMensaje(), n.getParaUsuarioId(),
					n.getPedidoId(), n.isLeida(), n.getFecha());
		}
	}
}
