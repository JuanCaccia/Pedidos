package com.sistema.usuario.port.out;

import com.sistema.usuario.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

	Usuario save(Usuario usuario);

	Optional<Usuario> findById(Long id);

	Optional<Usuario> findByEmail(String email);

	List<Usuario> findAll();
}
