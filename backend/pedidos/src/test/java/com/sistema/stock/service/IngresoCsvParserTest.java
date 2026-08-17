package com.sistema.stock.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngresoCsvParserTest {

	private final IngresoCsvParser parser = new IngresoCsvParser();

	@Test
	void parseaCsvConSeparadorComaYColumnasObligatorias() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario\nHAR-001,100,12.50\nACE-001,5,30");

		assertTrue(r.errores().isEmpty());
		assertEquals(2, r.filas().size());
		FilaIngresoCsv f1 = r.filas().get(0);
		assertEquals(1, f1.numeroFila());
		assertEquals("HAR-001", f1.sku());
		assertEquals(0, new BigDecimal("100").compareTo(f1.cantidad()));
		assertEquals(0, new BigDecimal("12.50").compareTo(f1.precioUnitario()));
		assertEquals(null, f1.fechaVencimiento());
		assertEquals(null, f1.codigoLote());
		assertEquals(2, r.filas().get(1).numeroFila());
	}

	@Test
	void parseaCsvConSeparadorPuntoYComaYColumnasOpcionales() {
		CsvParseResult r = parser.parsear(
				"sku;cantidad;precioUnitario;fechaVencimiento;codigoLote\n"
						+ "HAR-001;100;12,50;2026-12-31;LOTE-A\n"
						+ "ACE-001;5;30;;");

		assertTrue(r.errores().isEmpty());
		FilaIngresoCsv f1 = r.filas().get(0);
		assertEquals(0, new BigDecimal("12.50").compareTo(f1.precioUnitario()));
		assertEquals(LocalDate.of(2026, 12, 31), f1.fechaVencimiento());
		assertEquals("LOTE-A", f1.codigoLote());
		assertEquals(null, r.filas().get(1).fechaVencimiento());
		assertEquals(null, r.filas().get(1).codigoLote());
	}

	@Test
	void columnasEnOtroOrdenSeResuelvenPorNombre() {
		CsvParseResult r = parser.parsear("precioUnitario,cantidad,sku\n15,100,HAR-001");

		assertTrue(r.errores().isEmpty());
		assertEquals("HAR-001", r.filas().get(0).sku());
		assertEquals(0, new BigDecimal("100").compareTo(r.filas().get(0).cantidad()));
		assertEquals(0, new BigDecimal("15").compareTo(r.filas().get(0).precioUnitario()));
	}

	@Test
	void recolectaErroresPorFilaSinDetenerseEnLaPrimeraMala() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario,fechaVencimiento\n"
				+ "A,0,10,\n"
				+ "B,5,-3,\n"
				+ "C,abc,1,\n"
				+ "D,2,1,2020-99-99\n"
				+ "E,10,5,");

		assertEquals(1, r.filas().size());
		assertEquals(4, r.errores().size());
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 1: la cantidad debe ser mayor a cero")));
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 2: el precio unitario no puede ser negativo")));
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 3: cantidad inválida")));
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 4: fecha de vencimiento inválida")));
	}

	@Test
	void cantidadObligatoriaYSkuObligatorio() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario\n,10,1\nA,,1");

		assertTrue(r.filas().isEmpty());
		assertEquals(2, r.errores().size());
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 1: el sku es obligatorio")));
		assertTrue(r.errores().stream().anyMatch(e -> e.startsWith("fila 2: la cantidad es obligatoria")));
	}

	@Test
	void encabezadoInvalidoDevuelveError() {
		CsvParseResult r = parser.parsear("nombre,precio\nA,10");

		assertTrue(r.filas().isEmpty());
		assertEquals(1, r.errores().size());
		assertTrue(r.errores().get(0).contains("encabezado"));
	}

	@Test
	void csvVacioDevuelveError() {
		CsvParseResult vacio = parser.parsear("");
		assertEquals(1, vacio.errores().size());
		assertTrue(vacio.errores().get(0).contains("vacío"));

		CsvParseResult sinDatos = parser.parsear("sku,cantidad,precioUnitario\n");
		assertEquals(1, sinDatos.errores().size());
		assertTrue(sinDatos.errores().get(0).contains("filas de datos"));
	}

	@Test
	void ignoraFilasEnBlanco() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario\nA,10,1\n\nB,5,2\n");

		assertTrue(r.errores().isEmpty());
		assertEquals(2, r.filas().size());
		assertEquals(1, r.filas().get(0).numeroFila());
		assertEquals(2, r.filas().get(1).numeroFila());
	}

	@Test
	void separadorPorComaConDecimalesDePunto() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario\nA,1.5,3.25");

		assertTrue(r.errores().isEmpty());
		assertEquals(0, new BigDecimal("1.5").compareTo(r.filas().get(0).cantidad()));
		assertEquals(0, new BigDecimal("3.25").compareTo(r.filas().get(0).precioUnitario()));
	}

	@Test
	void listaDeFilasNumeradasDesdeUno() {
		CsvParseResult r = parser.parsear("sku,cantidad,precioUnitario\nA,1,1\nB,2,2\nC,3,3");

		assertTrue(r.errores().isEmpty());
		assertEquals(List.of(1, 2, 3), r.filas().stream().map(FilaIngresoCsv::numeroFila).toList());
	}
}
