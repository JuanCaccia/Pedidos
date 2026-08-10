package com.sistema.usuario.adapter.in.web;

import com.sistema.usuario.adapter.in.web.dto.LoginRequest;
import com.sistema.usuario.adapter.in.web.dto.LoginResponse;
import com.sistema.usuario.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@SecurityRequirements
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Autentica un usuario y devuelve un token JWT")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		AuthService.LoginResultado resultado = authService.login(request.email(), request.password());
		return ResponseEntity.ok(LoginResponse.of(resultado.token(), resultado.usuario()));
	}
}
