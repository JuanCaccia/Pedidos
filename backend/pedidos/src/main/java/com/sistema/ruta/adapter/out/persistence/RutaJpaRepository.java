package com.sistema.ruta.adapter.out.persistence;

import com.sistema.ruta.model.EstadoRuta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RutaJpaRepository extends JpaRepository<RutaJpaEntity, Long> {

	List<RutaJpaEntity> findByFechaJornada(LocalDate fechaJornada);

	List<RutaJpaEntity> findByRepartidorId(Long repartidorId);

	List<RutaJpaEntity> findByEstado(EstadoRuta estado);
}
