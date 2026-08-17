package com.sistema.common.exception;

import java.util.List;

public class CsvImportException extends RuntimeException {

	private final List<String> errores;

	public CsvImportException(List<String> errores) {
		super("Errores en la importación CSV");
		this.errores = errores;
	}

	public List<String> getErrores() {
		return errores;
	}
}
