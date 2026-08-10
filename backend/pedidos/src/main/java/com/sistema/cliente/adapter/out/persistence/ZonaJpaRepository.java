package com.sistema.cliente.adapter.out.persistence;

import com.sistema.common.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZonaJpaRepository extends JpaRepository<Zona, Long> {

	Optional<Zona> findByNombre(String nombre);
}
