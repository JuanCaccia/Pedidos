package com.sistema.usuario.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
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
			throw new BusinessException("USUARIO_EMAIL_DUPLICADO", "A user with that email already exists");
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

	private Usuario obtenerO404(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new NotFoundException("User not found: " + usuarioId));
	}

	private void validarDatosComunes(String nombre, String email, String password, Set<Rol> roles) {
		if (nombre == null || nombre.isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Name is required");
		}
		if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new BusinessException("VALIDATION_ERROR", "A valid email is required");
		}
		if (password == null || password.length() < 6) {
			throw new BusinessException("VALIDATION_ERROR", "Password must be at least 6 characters");
		}
		if (roles == null || roles.isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "At least one role is required");
		}
	}
}
