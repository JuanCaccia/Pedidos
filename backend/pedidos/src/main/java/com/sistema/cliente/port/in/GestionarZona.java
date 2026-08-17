package com.sistema.cliente.port.in;

import com.sistema.common.model.Zona;

import java.util.List;
import java.util.Optional;

public interface GestionarZona {

	record CrearZonaCommand(String nombre) {
	}

	record ActualizarZonaCommand(Long zonaId, String nombre) {
	}

	Zona crearZona(CrearZonaCommand command);

	Zona actualizarZona(ActualizarZonaCommand command);

	void desactivarZona(Long zonaId);

	void reactivarZona(Long zonaId);

	List<Zona> listarTodas();

	Optional<Zona> buscarPorId(Long id);
}
