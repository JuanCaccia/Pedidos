package com.sistema.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

	private static final String SECRET_DEFAULT_INSEGURO = "cambiar-en-produccion-esta-clave-secreta-jwt-2026";

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms:28800000}") long expirationMs, Environment environment) {
		boolean perfilProd = environment.getActiveProfiles().length > 0
				&& Arrays.asList(environment.getActiveProfiles()).contains("prod");
		if (perfilProd && (secret == null || secret.isBlank() || SECRET_DEFAULT_INSEGURO.equals(secret))) {
			throw new IllegalStateException(
					"JWT_SECRET no configurado: en perfil prod se debe proveer por variable de entorno y no puede usar el default inseguro");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generarToken(Long usuarioId, String email, List<String> roles) {
		Date ahora = new Date();
		return Jwts.builder()
				.subject(String.valueOf(usuarioId))
				.claim("email", email)
				.claim("roles", roles)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	public Long obtenerUsuarioId(String token) {
		Claims claims = Jwts.parser().verifyWith(key).build()
				.parseSignedClaims(token).getPayload();
		return Long.valueOf(claims.getSubject());
	}

	public boolean esValido(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
