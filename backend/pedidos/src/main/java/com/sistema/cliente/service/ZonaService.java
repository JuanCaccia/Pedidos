package com.sistema.cliente.service;

import com.sistema.cliente.port.in.GestionarZona;
import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
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
		String nombre = validarNombre(command.nombre());
		zonaRepository.findByNombre(nombre).ifPresent(z -> {
			throw new BusinessException("ZONA_NOMBRE_DUPLICADO", "Ya existe una zona con ese nombre");
		});
		return zonaRepository.save(new Zona(nombre));
	}

	@Override
	@Transactional
	public Zona actualizarZona(ActualizarZonaCommand command) {
		Zona zona = obtenerO404(command.zonaId());
		String nombre = validarNombre(command.nombre());
		zonaRepository.findByNombre(nombre)
				.filter(z -> !z.getId().equals(zona.getId()))
				.ifPresent(z -> {
					throw new BusinessException("ZONA_NOMBRE_DUPLICADO", "Ya existe una zona con ese nombre");
				});
		zona.renombrar(nombre);
		return zonaRepository.save(zona);
	}

	@Override
	@Transactional
	public void desactivarZona(Long zonaId) {
		Zona zona = obtenerO404(zonaId);
		zona.desactivar();
		zonaRepository.save(zona);
	}

	@Override
	@Transactional
	public void reactivarZona(Long zonaId) {
		Zona zona = obtenerO404(zonaId);
		zona.reactivar();
		zonaRepository.save(zona);
	}

	@Override
	public List<Zona> listarTodas() {
		return zonaRepository.findAll();
	}

	@Override
	public Optional<Zona> buscarPorId(Long id) {
		return zonaRepository.findById(id);
	}

	private Zona obtenerO404(Long zonaId) {
		return zonaRepository.findById(zonaId)
				.orElseThrow(() -> new NotFoundException("Zona no encontrada: " + zonaId));
	}

	private String validarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre de la zona es obligatorio");
		}
		String normalizado = nombre.trim();
		if (normalizado.length() > 100) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre de la zona no puede superar los 100 caracteres");
		}
		return normalizado;
	}
}
