package com.sistema.usuario.adapter.out.ruta;

import com.sistema.ruta.port.out.RepartidorGateway;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.port.out.UsuarioRepository;
import org.springframework.stereotype.Component;

@Component
public class RepartidorGatewayImpl implements RepartidorGateway {

	private final UsuarioRepository usuarioRepository;

	public RepartidorGatewayImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public boolean existeRepartidor(Long usuarioId) {
		return usuarioRepository.findById(usuarioId)
				.map(u -> u.isActivo() && u.tieneRol(Rol.REPARTIDOR))
				.orElse(false);
	}
}
