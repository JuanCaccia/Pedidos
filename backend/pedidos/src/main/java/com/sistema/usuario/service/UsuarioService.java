package com.sistema.usuario.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import com.sistema.usuario.port.in.GestionarUsuario;
import com.sistema.usuario.port.out.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UsuarioService implements GestionarUsuario, ConsultarUsuario {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public Usuario crearUsuario(CrearUsuarioCommand command) {
		validarDatosComunes(command.nombre(), command.email(), command.password(), command.roles());
		String emailNormalizado = command.email().trim().toLowerCase();
		usuarioRepository.findByEmail(emailNormalizado).ifPresent(u -> {
			throw new BusinessException("USUARIO_EMAIL_DUPLICADO", "Ya existe un usuario con ese email");
		});
		Usuario usuario = new Usuario(command.nombre().trim(), emailNormalizado,
				passwordEncoder.encode(command.password()), command.roles());
		return usuarioRepository.save(usuario);
	}

	@Override
	@Transactional
	public void asignarRoles(AsignarRolesCommand command) {
		Usuario usuario = obtenerO404(command.usuarioId());
		usuario.asignarRoles(command.roles());
		usuarioRepository.save(usuario);
	}

	@Override
	@Transactional
	public void desactivarUsuario(Long usuarioId) {
		Usuario usuario = obtenerO404(usuarioId);
		usuario.desactivar();
		usuarioRepository.save(usuario);
	}

	@Override
	@Transactional
	public void reactivarUsuario(Long usuarioId) {
		Usuario usuario = obtenerO404(usuarioId);
		usuario.reactivar();
		usuarioRepository.save(usuario);
	}

	@Override
	public Optional<Usuario> buscarPorId(Long id) {
		return usuarioRepository.findById(id);
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return usuarioRepository.findByEmail(email.trim().toLowerCase());
	}

	@Override
	public List<Usuario> listarTodos() {
		return usuarioRepository.findAll();
	}

	@Override
	public PageResponse<Usuario> listarPaginado(String q, int page, int size) {
		return usuarioRepository.buscar(q, page, size);
	}

	@Override
	@Transactional
	public void cambiarPassword(CambiarPasswordCommand command) {
		boolean esMismoUsuario = command.actor() != null && command.actor().getId() != null
				&& command.actor().getId().equals(command.usuarioId());
		if (!esMismoUsuario && (command.actor() == null || !command.actor().tieneRol(Rol.ADMINISTRATIVO))) {
			throw new BusinessException("SIN_PERMISO", "No tiene permisos para cambiar esta contraseña");
		}
		if (command.nuevaPassword() == null || command.nuevaPassword().length() < 6) {
			throw new BusinessException("VALIDATION_ERROR", "La contraseña debe tener al menos 6 caracteres");
		}
		Usuario usuario = usuarioRepository.findById(command.usuarioId())
				.orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + command.usuarioId()));
		usuario.setPasswordHash(passwordEncoder.encode(command.nuevaPassword()));
		usuarioRepository.save(usuario);
	}

	private Usuario obtenerO404(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + usuarioId));
	}

	private void validarDatosComunes(String nombre, String email, String password, Set<Rol> roles) {
		if (nombre == null || nombre.isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre es obligatorio");
		}
		if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new BusinessException("VALIDATION_ERROR", "Se requiere un email válido");
		}
		if (password == null || password.length() < 6) {
			throw new BusinessException("VALIDATION_ERROR", "La contraseña debe tener al menos 6 caracteres");
		}
		if (roles == null || roles.isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "Se requiere al menos un rol");
		}
	}
}
