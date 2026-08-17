package com.sistema.stock.port.in;

import com.sistema.stock.model.Lote;

import java.util.List;

public interface ImportarIngresoCsv {

	List<Lote> importar(String csv, Long proveedorId);
}
