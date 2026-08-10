package com.sistema.ruta.port.in;

import com.sistema.ruta.model.EstadoRuta;
import com.sistema.ruta.model.Ruta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsultarRuta {

	Optional<Ruta> buscarPorId(Long id);

	List<Ruta> listarTodos();

	List<Ruta> listarPorFecha(LocalDate fechaJornada);

	List<Ruta> listarPorRepartidor(Long repartidorId);

	List<Ruta> listarPorEstado(EstadoRuta estado);
}
