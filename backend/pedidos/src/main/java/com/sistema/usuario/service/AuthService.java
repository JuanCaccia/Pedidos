package com.sistema.usuario.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.security.JwtService;
import com.sistema.usuario.model.Usuario;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final ConsultarUsuario consultarUsuario;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(ConsultarUsuario consultarUsuario, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.consultarUsuario = consultarUsuario;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResultado login(String email, String password) {
		Usuario usuario = consultarUsuario.buscarPorEmail(email)
				.orElseThrow(() -> new BusinessException("AUTH_INVALIDO", "Credenciales invalidas"));
		if (!usuario.isActivo()) {
			throw new BusinessException("AUTH_INACTIVO", "Usuario desactivado");
		}
		if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
			throw new BusinessException("AUTH_INVALIDO", "Credenciales invalidas");
		}
		String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(),
				usuario.getRoles().stream().map(Enum::name).toList());
		return new LoginResultado(token, usuario);
	}

	public record LoginResultado(String token, Usuario usuario) {
	}
}
