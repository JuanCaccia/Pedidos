package com.sistema.stock.port.in;

import com.sistema.stock.model.Lote;

public interface DescartarLote {

	Lote descartar(Long loteId);
}
