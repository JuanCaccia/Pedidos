package com.sistema.stock.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser de CSV para ingreso/recepción de stock. Formato por fila:
 *
 * <pre>
 * sku, cantidad, precioUnitario, fechaVencimiento(opcional), codigoLote(opcional)
 * </pre>
 *
 * La primera línea es obligatoria y debe ser el encabezado (el orden de las
 * columnas no importa, se resuelven por nombre). El separador puede ser ';' o
 * ',' (se detecta automáticamente); con ';' los decimales pueden usar ','.
 *
 * Los errores se recolectan por fila y no interrumpen el parseo.
 */
@Component
public class IngresoCsvParser {

	private static final List<String> OBLIGATORIAS = List.of("sku", "cantidad", "preciounitario");

	public CsvParseResult parsear(String csv) {
		List<String> errores = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return new CsvParseResult(List.of(), List.of("El archivo CSV está vacío"));
		}
		char sep = detectarSeparador(csv);
		String[] lineas = csv.split("\r?\n");
		if (lineas.length == 0 || lineas[0].isBlank()) {
			return new CsvParseResult(List.of(), List.of("El archivo CSV debe tener una fila de encabezado"));
		}
		Map<String, Integer> idx = indiceColumnas(lineas[0], sep);
		if (idx == null) {
			return new CsvParseResult(List.of(),
					List.of("El encabezado debe contener las columnas: sku, cantidad, precioUnitario "
							+ "(fechaVencimiento y codigoLote son opcionales)"));
		}

		List<FilaIngresoCsv> filas = new ArrayList<>();
		int fila = 0;
		for (int i = 1; i < lineas.length; i++) {
			String linea = lineas[i];
			if (linea.isBlank()) {
				continue;
			}
			fila++;
			String base = "fila " + fila + ": ";
			boolean ok = true;
			String[] cols = linea.split(Character.toString(sep), -1);

			String sku = col(idx, cols, "sku");
			if (sku.isBlank()) {
				errores.add(base + "el sku es obligatorio");
				ok = false;
			}

			BigDecimal cantidad = null;
			String cantidadRaw = col(idx, cols, "cantidad");
			if (cantidadRaw.isBlank()) {
				errores.add(base + "la cantidad es obligatoria");
				ok = false;
			} else {
				try {
					cantidad = parseDecimal(cantidadRaw, sep);
				} catch (NumberFormatException ex) {
					errores.add(base + "cantidad inválida: '" + cantidadRaw + "'");
					ok = false;
				}
			}

			BigDecimal precio = null;
			String precioRaw = col(idx, cols, "precioUnitario");
			if (!precioRaw.isBlank()) {
				try {
					precio = parseDecimal(precioRaw, sep);
				} catch (NumberFormatException ex) {
					errores.add(base + "precio inválido: '" + precioRaw + "'");
					ok = false;
				}
			}

			LocalDate venc = null;
			String vencRaw = col(idx, cols, "fechaVencimiento");
			if (!vencRaw.isBlank()) {
				try {
					venc = LocalDate.parse(vencRaw);
				} catch (DateTimeParseException ex) {
					errores.add(base + "fecha de vencimiento inválida: '" + vencRaw + "'");
					ok = false;
				}
			}

			if (cantidad != null && cantidad.signum() <= 0) {
				errores.add(base + "la cantidad debe ser mayor a cero");
				ok = false;
			}
			if (precio != null && precio.signum() < 0) {
				errores.add(base + "el precio unitario no puede ser negativo");
				ok = false;
			}

			if (!ok) {
				continue;
			}
			String codigoLote = col(idx, cols, "codigoLote");
			filas.add(new FilaIngresoCsv(fila, sku, cantidad, precio, venc,
					codigoLote.isBlank() ? null : codigoLote));
		}

		if (filas.isEmpty() && errores.isEmpty()) {
			errores.add("El archivo CSV no contiene filas de datos");
		}
		return new CsvParseResult(filas, errores);
	}

	private char detectarSeparador(String csv) {
		long conPunto = csv.chars().filter(c -> c == ';').count();
		long conComa = csv.chars().filter(c -> c == ',').count();
		return conPunto >= conComa && conPunto > 0 ? ';' : ',';
	}

	private Map<String, Integer> indiceColumnas(String header, char sep) {
		String[] cols = header.split(Character.toString(sep), -1);
		Map<String, Integer> idx = new LinkedHashMap<>();
		for (int i = 0; i < cols.length; i++) {
			idx.put(cols[i].trim().toLowerCase(), i);
		}
		for (String requerida : OBLIGATORIAS) {
			if (!idx.containsKey(requerida)) {
				return null;
			}
		}
		return idx;
	}

	private String col(Map<String, Integer> idx, String[] cols, String nombre) {
		Integer i = idx.get(nombre.toLowerCase());
		if (i == null || i >= cols.length) {
			return "";
		}
		return cols[i].trim();
	}

	private BigDecimal parseDecimal(String raw, char sep) {
		String v = raw.trim();
		if (sep == ';') {
			v = v.replace(',', '.');
		}
		return new BigDecimal(v);
	}
}
