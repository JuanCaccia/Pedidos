package com.sistema.cliente.port.out;

import com.sistema.common.model.Zona;

import java.util.List;
import java.util.Optional;

public interface ZonaRepository {

	Zona save(Zona zona);

	Optional<Zona> findById(Long id);

	Optional<Zona> findByNombre(String nombre);

	List<Zona> findAll();
}
