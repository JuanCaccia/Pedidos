package com.sistema.stock.service;

import com.sistema.common.exception.CsvImportException;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.port.in.ImportarIngresoCsv;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.port.out.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngresoCsvService implements ImportarIngresoCsv {

	private final IngresoCsvParser parser;
	private final ItemRepository itemRepository;
	private final RegistrarIngreso registrarIngreso;

	public IngresoCsvService(IngresoCsvParser parser, ItemRepository itemRepository, RegistrarIngreso registrarIngreso) {
		this.parser = parser;
		this.itemRepository = itemRepository;
		this.registrarIngreso = registrarIngreso;
	}

	@Override
	@Transactional
	public List<Lote> importar(String csv, Long proveedorId) {
		CsvParseResult resultado = parser.parsear(csv);
		List<String> errores = new ArrayList<>(resultado.errores());
		for (FilaIngresoCsv fila : resultado.filas()) {
			Item item = itemRepository.findBySku(fila.sku().trim().toUpperCase()).orElse(null);
			if (item == null) {
				errores.add("fila " + fila.numeroFila() + ": sku inexistente " + fila.sku());
			} else if (!item.isActivo()) {
				errores.add("fila " + fila.numeroFila() + ": el item " + fila.sku() + " está inactivo");
			}
		}
		if (!errores.isEmpty()) {
			throw new CsvImportException(errores);
		}

		List<Lote> lotes = new ArrayList<>();
		for (FilaIngresoCsv fila : resultado.filas()) {
			Item item = itemRepository.findBySku(fila.sku().trim().toUpperCase()).orElseThrow();
			lotes.add(registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
					item.getId(), fila.codigoLote(), fila.fechaVencimiento(), fila.cantidad(),
					"Ingreso por CSV", proveedorId, fila.precioUnitario())));
		}
		return lotes;
	}
}
