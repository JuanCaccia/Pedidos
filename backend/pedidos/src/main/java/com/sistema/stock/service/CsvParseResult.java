package com.sistema.stock.service;

import java.util.List;

public record CsvParseResult(List<FilaIngresoCsv> filas, List<String> errores) {
}
