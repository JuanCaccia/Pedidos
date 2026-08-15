package com.sistema.usuario.adapter.out.notificacion;

import com.sistema.notificacion.port.out.UsuarioGateway;
import com.sistema.usuario.port.out.UsuarioRepository;
import org.springframework.stereotype.Component;

@Component("notificacionUsuarioGateway")
public class UsuarioGatewayImpl implements UsuarioGateway {

	private final UsuarioRepository usuarioRepository;

	public UsuarioGatewayImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public boolean existeUsuario(Long usuarioId) {
		return usuarioRepository.findById(usuarioId).isPresent();
	}
}
