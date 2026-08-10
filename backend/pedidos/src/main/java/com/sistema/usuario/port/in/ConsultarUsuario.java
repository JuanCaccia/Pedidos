package com.sistema.usuario.port.in;

import com.sistema.usuario.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface ConsultarUsuario {

	Optional<Usuario> buscarPorId(Long id);

	Optional<Usuario> buscarPorEmail(String email);

	List<Usuario> listarTodos();
}
