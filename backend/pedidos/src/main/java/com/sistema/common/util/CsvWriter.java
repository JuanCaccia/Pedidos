package com.sistema.common.util;

import java.util.List;

public final class CsvWriter {

	private CsvWriter() {
	}

	public static String escribir(List<String> headers, List<List<String>> filas) {
		StringBuilder sb = new StringBuilder();
		sb.append(fila(headers)).append('\n');
		for (List<String> fila : filas) {
			sb.append(fila(fila)).append('\n');
		}
		return sb.toString();
	}

	private static String fila(List<String> celdas) {
		return celdas.stream().map(CsvWriter::celda).collect(java.util.stream.Collectors.joining(","));
	}

	private static String celda(String valor) {
		if (valor == null) {
			return "";
		}
		String v = valor.replace("\"", "\"\"");
		if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
			return "\"" + v + "\"";
		}
		return v;
	}
}
