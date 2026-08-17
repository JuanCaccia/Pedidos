package com.sistema.stock.adapter.in.web.dto;

import com.sistema.stock.model.Lote;

import java.util.List;

public record ImportacionCsvResponse(List<LoteImportadoResponse> lotesCreados, List<String> errores) {

	public static ImportacionCsvResponse de(List<Lote> lotes) {
		return new ImportacionCsvResponse(
				lotes.stream().map(LoteImportadoResponse::from).toList(), List.of());
	}
}
