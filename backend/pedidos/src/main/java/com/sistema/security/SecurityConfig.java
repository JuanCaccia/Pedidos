package com.sistema.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/login", "/health", "/actuator/health", "/actuator/health/**",
								"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.PUT, "/usuarios/*/password").authenticated()
						.requestMatchers("/usuarios/**").hasRole("ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos").hasAnyRole("VENDEDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/confirmar").hasAnyRole("VENDEDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/despachar").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/entregas").hasAnyRole("REPARTIDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/reagendar").hasAnyRole("REPARTIDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/agregar-stock").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/pedidos/*/marcar-faltante").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.GET, "/notificaciones/**").hasAnyRole("ADMINISTRATIVO", "ENCARGADO_DEPOSITO")
						.requestMatchers(HttpMethod.POST, "/notificaciones/*/leer").hasAnyRole("ADMINISTRATIVO", "ENCARGADO_DEPOSITO")
						.requestMatchers(HttpMethod.POST, "/stock/ingresos", "/stock/mermas", "/stock/lotes/*/descartar").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/stock/ajustes").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers("/rutas/**").hasAnyRole("REPARTIDOR", "ADMINISTRATIVO")
						.requestMatchers("/reportes/stock", "/reportes/stock/exportar.csv").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers("/reportes/**").hasRole("ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/items", "/items/**").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.PUT, "/items/*", "/items/*/desactivar", "/items/*/reactivar").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.GET, "/categorias", "/categorias/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/categorias").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.PUT, "/categorias/*").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.PATCH, "/categorias/*/desactivar", "/categorias/*/reactivar").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/clientes").hasAnyRole("VENDEDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.PUT, "/clientes/*", "/clientes/*/desactivar", "/clientes/*/reactivar").hasAnyRole("VENDEDOR", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/cobranzas").hasAnyRole("VENDEDOR", "ADMINISTRATIVO")
						.requestMatchers("/proveedores/**", "/ordenes-compra/**").hasAnyRole("ENCARGADO_DEPOSITO", "ADMINISTRATIVO")
						.requestMatchers(HttpMethod.POST, "/sustituciones").hasAnyRole("REPARTIDOR", "ADMINISTRATIVO")
						.requestMatchers("/actuator/**").hasRole("ADMINISTRATIVO")
						.anyRequest().authenticated())
				.exceptionHandling(eh -> eh
						.authenticationEntryPoint((request, response, ex) -> {
							response.setStatus(401);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							response.setCharacterEncoding("UTF-8");
							response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Se requiere autenticacion\"}");
						})
						.accessDeniedHandler((request, response, ex) -> {
							response.setStatus(403);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							response.setCharacterEncoding("UTF-8");
							response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"No tiene permisos para esta operacion\"}");
						}))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
