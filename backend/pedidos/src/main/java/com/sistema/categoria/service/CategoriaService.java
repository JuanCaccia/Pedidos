package com.sistema.categoria.service;

import com.sistema.categoria.model.Categoria;
import com.sistema.categoria.port.in.ConsultarCategoria;
import com.sistema.categoria.port.in.GestionarCategoria;
import com.sistema.categoria.port.out.CategoriaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoriaService implements GestionarCategoria, ConsultarCategoria {

	private final CategoriaRepository categoriaRepository;

	public CategoriaService(CategoriaRepository categoriaRepository) {
		this.categoriaRepository = categoriaRepository;
	}

	@Override
	@Transactional
	public Categoria crear(CrearCategoriaCommand command) {
		String nombre = validarNombre(command.nombre());
		categoriaRepository.findByNombre(nombre).ifPresent(c -> {
			throw new BusinessException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
		});
		return categoriaRepository.save(new Categoria(nombre));
	}

	@Override
	@Transactional
	public Categoria actualizar(ActualizarCategoriaCommand command) {
		Categoria categoria = obtenerO404(command.categoriaId());
		String nombre = validarNombre(command.nombre());
		categoriaRepository.findByNombre(nombre)
				.filter(c -> !c.getId().equals(categoria.getId()))
				.ifPresent(c -> {
					throw new BusinessException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
				});
		categoria.renombrar(nombre);
		return categoriaRepository.save(categoria);
	}

	@Override
	@Transactional
	public void desactivar(Long categoriaId) {
		Categoria categoria = obtenerO404(categoriaId);
		categoria.desactivar();
		categoriaRepository.save(categoria);
	}

	@Override
	@Transactional
	public void reactivar(Long categoriaId) {
		Categoria categoria = obtenerO404(categoriaId);
		categoria.reactivar();
		categoriaRepository.save(categoria);
	}

	@Override
	public List<Categoria> listarActivas() {
		return categoriaRepository.findByActivoTrue();
	}

	@Override
	public List<Categoria> listarTodas() {
		return categoriaRepository.findAll();
	}

	@Override
	public Optional<Categoria> buscarPorId(Long id) {
		return categoriaRepository.findById(id);
	}

	private Categoria obtenerO404(Long categoriaId) {
		return categoriaRepository.findById(categoriaId)
				.orElseThrow(() -> new NotFoundException("Categoría no encontrada: " + categoriaId));
	}

	private String validarNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre de la categoría es obligatorio");
		}
		String normalizado = nombre.trim();
		if (normalizado.length() > 100) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre de la categoría no puede superar los 100 caracteres");
		}
		return normalizado;
	}
}
