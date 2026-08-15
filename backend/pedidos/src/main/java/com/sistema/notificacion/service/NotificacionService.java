package com.sistema.notificacion.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.notificacion.model.Notificacion;
import com.sistema.notificacion.port.in.ConsultarNotificacion;
import com.sistema.notificacion.port.in.GestionarNotificacion;
import com.sistema.notificacion.port.out.NotificacionRepository;
import com.sistema.notificacion.port.out.UsuarioGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificacionService implements GestionarNotificacion, ConsultarNotificacion {

	private final NotificacionRepository notificacionRepository;
	private final UsuarioGateway usuarioGateway;

	public NotificacionService(NotificacionRepository notificacionRepository, UsuarioGateway usuarioGateway) {
		this.notificacionRepository = notificacionRepository;
		this.usuarioGateway = usuarioGateway;
	}

	@Override
	@Transactional
	public Notificacion notificar(NotificarCommand command) {
		if (command.tipo() == null || command.tipo().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El tipo es obligatorio");
		}
		if (command.mensaje() == null || command.mensaje().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El mensaje es obligatorio");
		}
		if (command.paraUsuarioId() == null || !usuarioGateway.existeUsuario(command.paraUsuarioId())) {
			throw new BusinessException("USUARIO_INEXISTENTE", "El usuario destinatario no existe");
		}
		Notificacion notificacion = new Notificacion(command.tipo(), command.mensaje(), command.paraUsuarioId(),
				command.pedidoId());
		notificacion.setFecha(LocalDateTime.now());
		return notificacionRepository.save(notificacion);
	}

	@Override
	@Transactional
	public void marcarLeida(Long notificacionId, Long actorUsuarioId) {
		notificacionRepository.findByParaUsuarioId(actorUsuarioId).stream()
				.filter(n -> n.getId().equals(notificacionId))
				.findFirst()
				.ifPresent(n -> {
					n.setLeida(true);
					notificacionRepository.save(n);
				});
	}

	@Override
	public List<Notificacion> listar(Long paraUsuarioId, boolean soloNoLeidas) {
		return notificacionRepository.findByParaUsuarioId(paraUsuarioId).stream()
				.filter(n -> !soloNoLeidas || !n.isLeida())
				.toList();
	}

	@Override
	public long contarNoLeidas(Long paraUsuarioId) {
		return notificacionRepository.findByParaUsuarioId(paraUsuarioId).stream()
				.filter(n -> !n.isLeida())
				.count();
	}
}
