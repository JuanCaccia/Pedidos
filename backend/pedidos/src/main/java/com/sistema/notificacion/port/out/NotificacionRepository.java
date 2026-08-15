package com.sistema.notificacion.port.out;

import com.sistema.notificacion.model.Notificacion;

import java.util.List;

public interface NotificacionRepository {

	Notificacion save(Notificacion n);

	List<Notificacion> findByParaUsuarioId(Long paraUsuarioId);
}
