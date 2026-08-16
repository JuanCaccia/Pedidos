package com.sistema.config;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
@Profile({"dev", "test"})
public class TestResetController {

	private final TestResetDataCleaner cleaner;
	private final DataSeeder dataSeeder;

	public TestResetController(TestResetDataCleaner cleaner, DataSeeder dataSeeder) {
		this.cleaner = cleaner;
		this.dataSeeder = dataSeeder;
	}

	@PostMapping("/reset")
	public ResponseEntity<Map<String, String>> reset() {
		cleaner.limpiar();
		dataSeeder.seed();
		return ResponseEntity.ok(Map.of("status", "RESET_OK"));
	}
}
