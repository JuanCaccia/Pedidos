package com.sistema.notificacion.port.in;

import com.sistema.notificacion.model.Notificacion;

public interface GestionarNotificacion {

	record NotificarCommand(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId) {
	}

	Notificacion notificar(NotificarCommand command);

	void marcarLeida(Long notificacionId, Long actorUsuarioId);
}
