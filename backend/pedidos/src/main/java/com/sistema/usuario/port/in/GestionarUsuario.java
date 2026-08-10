package com.sistema.usuario.port.in;

import com.sistema.usuario.model.Rol;
import com.sistema.usuario.model.Usuario;

import java.util.Set;

public interface GestionarUsuario {

	record CrearUsuarioCommand(String nombre, String email, String password, Set<Rol> roles) {
	}

	record AsignarRolesCommand(Long usuarioId, Set<Rol> roles) {
	}

	record CambiarPasswordCommand(Long usuarioId, String nuevaPassword, com.sistema.usuario.model.Usuario actor) {
	}

	Usuario crearUsuario(CrearUsuarioCommand command);

	void asignarRoles(AsignarRolesCommand command);

	void desactivarUsuario(Long usuarioId);

	void reactivarUsuario(Long usuarioId);

	void cambiarPassword(CambiarPasswordCommand command);
}
