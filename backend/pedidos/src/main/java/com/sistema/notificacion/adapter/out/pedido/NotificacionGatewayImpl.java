package com.sistema.notificacion.adapter.out.pedido;

import com.sistema.notificacion.port.in.GestionarNotificacion;
import com.sistema.pedido.port.out.NotificacionGateway;
import org.springframework.stereotype.Component;

@Component
public class NotificacionGatewayImpl implements NotificacionGateway {

	private final GestionarNotificacion gestionarNotificacion;

	public NotificacionGatewayImpl(GestionarNotificacion gestionarNotificacion) {
		this.gestionarNotificacion = gestionarNotificacion;
	}

	@Override
	public void notificar(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId) {
		gestionarNotificacion.notificar(new GestionarNotificacion.NotificarCommand(tipo, mensaje, paraUsuarioId, pedidoId));
	}
}
