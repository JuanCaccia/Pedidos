package com.sistema.compra.port.in;

import com.sistema.stock.model.Lote;

import java.util.List;

public interface ImportarRecepcionCsv {

	List<Lote> importar(Long ordenId, String csv);
}
