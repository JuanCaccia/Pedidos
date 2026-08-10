package com.sistema.cliente.service;

import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.model.Zona;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ZonaService implements GestionarZona {

	private final ZonaRepository zonaRepository;

	public ZonaService(ZonaRepository zonaRepository) {
		this.zonaRepository = zonaRepository;
	}

	@Override
	@Transactional
	public Zona crearZona(CrearZonaCommand command) {
		if (command.nombre() == null || command.nombre().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre de la zona es obligatorio");
		}
		String nombre = command.nombre().trim();
		zonaRepository.findByNombre(nombre).ifPresent(z -> {
			throw new BusinessException("ZONA_NOMBRE_DUPLICADO", "Ya existe una zona con ese nombre");
		});
		return zonaRepository.save(new Zona(nombre));
	}

	@Override
	public List<Zona> listarTodas() {
		return zonaRepository.findAll();
	}

	@Override
	public Optional<Zona> buscarPorId(Long id) {
		return zonaRepository.findById(id);
	}
}
