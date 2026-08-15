package com.sistema.notificacion.adapter.out.persistence;

import com.sistema.notificacion.model.Notificacion;

public class NotificacionMapper {

	public Notificacion toDomain(NotificacionJpaEntity entity) {
		Notificacion notificacion = new Notificacion(entity.getTipo(), entity.getMensaje(), entity.getParaUsuarioId(),
				entity.getPedidoId());
		notificacion.setId(entity.getId());
		notificacion.setLeida(entity.isLeida());
		notificacion.setFecha(entity.getFecha());
		return notificacion;
	}

	public NotificacionJpaEntity toJpa(Notificacion notificacion) {
		NotificacionJpaEntity entity = new NotificacionJpaEntity(notificacion.getTipo(), notificacion.getMensaje(),
				notificacion.getParaUsuarioId(), notificacion.getPedidoId(), notificacion.isLeida(),
				notificacion.getFecha());
		if (notificacion.getId() != null) {
			entity.setId(notificacion.getId());
		}
		return entity;
	}
}
