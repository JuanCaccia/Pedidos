package com.sistema.notificacion.port.in;

import com.sistema.notificacion.model.Notificacion;

import java.util.List;

public interface ConsultarNotificacion {

	List<Notificacion> listar(Long paraUsuarioId, boolean soloNoLeidas);

	long contarNoLeidas(Long paraUsuarioId);
}
