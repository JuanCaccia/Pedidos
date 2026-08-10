package com.sistema.ruta.port.out;

import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RutaRepository {

	Ruta save(Ruta ruta);

	Optional<Ruta> findById(Long id);

	List<Ruta> findAll();

	List<Ruta> findByFechaJornada(LocalDate fechaJornada);

	List<Ruta> findByRepartidorId(Long repartidorId);

	List<Ruta> findByEstado(EstadoRuta estado);
}
