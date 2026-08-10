package com.sistema.cliente.port.in;

import com.sistema.common.model.Zona;

import java.util.List;
import java.util.Optional;

public interface GestionarZona {

	record CrearZonaCommand(String nombre) {
	}

	Zona crearZona(CrearZonaCommand command);

	List<Zona> listarTodas();

	Optional<Zona> buscarPorId(Long id);
}
