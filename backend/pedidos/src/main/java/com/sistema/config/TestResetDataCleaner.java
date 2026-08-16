package com.sistema.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TestResetDataCleaner {

	private static final List<String> TABLAS = List.of(
			"sustitucion", "cobranza", "remito_linea", "remito",
			"notificacion", "ruta_pedido", "ruta",
			"movimiento_stock", "pedido_item", "pedido",
			"lote", "item", "categoria",
			"orden_compra_linea", "orden_compra", "proveedor",
			"cliente", "zona",
			"usuario_roles", "usuario");

	private final JdbcTemplate jdbcTemplate;

	public TestResetDataCleaner(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void limpiar() {
		jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", TABLAS) + " CASCADE");
	}
}
