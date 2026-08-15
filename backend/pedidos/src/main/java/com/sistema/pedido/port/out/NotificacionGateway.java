package com.sistema.pedido.port.out;

public interface NotificacionGateway {

	void notificar(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId);
}
