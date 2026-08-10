package com.sistema.common.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "Salud")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			body.put("status", "UP");
			body.put("db", "UP");
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			body.put("status", "DOWN");
			body.put("db", "DOWN");
			return ResponseEntity.status(503).body(body);
		}
	}
}
