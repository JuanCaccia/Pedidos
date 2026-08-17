package com.sistema.compra.service;

import com.sistema.common.exception.CsvImportException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.compra.model.EstadoOrdenCompra;
import com.sistema.compra.model.OrdenCompra;
import com.sistema.compra.model.OrdenCompraLinea;
import com.sistema.compra.port.in.ConsultarOrdenCompra;
import com.sistema.compra.port.in.GestionarOrdenCompra;
import com.sistema.compra.port.in.ImportarRecepcionCsv;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.port.out.ItemRepository;
import com.sistema.stock.service.CsvParseResult;
import com.sistema.stock.service.FilaIngresoCsv;
import com.sistema.stock.service.IngresoCsvParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecepcionCsvService implements ImportarRecepcionCsv {

	private final IngresoCsvParser parser;
	private final ItemRepository itemRepository;
	private final ConsultarOrdenCompra consultarOrdenCompra;
	private final GestionarOrdenCompra gestionarOrdenCompra;

	public RecepcionCsvService(IngresoCsvParser parser, ItemRepository itemRepository,
			ConsultarOrdenCompra consultarOrdenCompra, GestionarOrdenCompra gestionarOrdenCompra) {
		this.parser = parser;
		this.itemRepository = itemRepository;
		this.consultarOrdenCompra = consultarOrdenCompra;
		this.gestionarOrdenCompra = gestionarOrdenCompra;
	}

	@Override
	@Transactional
	public List<Lote> importar(Long ordenId, String csv) {
		OrdenCompra orden = consultarOrdenCompra.buscarPorId(ordenId)
				.orElseThrow(() -> new NotFoundException("Orden de compra no encontrada: " + ordenId));

		CsvParseResult resultado = parser.parsear(csv);
		List<String> errores = new ArrayList<>(resultado.errores());
		if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE
				&& orden.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
			errores.add("Solo se puede recibir una OC en PENDIENTE o RECIBIDA_PARCIAL");
		}

		List<GestionarOrdenCompra.RecepcionCsvLineaCommand> lineas = new ArrayList<>();
		for (FilaIngresoCsv fila : resultado.filas()) {
			Item item = itemRepository.findBySku(fila.sku().trim().toUpperCase()).orElse(null);
			if (item == null) {
				errores.add("fila " + fila.numeroFila() + ": sku inexistente " + fila.sku());
				continue;
			}
			OrdenCompraLinea linea = orden.lineaPorItemId(item.getId()).orElse(null);
			if (linea == null) {
				errores.add("fila " + fila.numeroFila() + ": el sku " + fila.sku() + " no está en la orden");
				continue;
			}
			if (fila.cantidad().compareTo(linea.restante()) > 0) {
				errores.add("fila " + fila.numeroFila() + ": la cantidad supera el restante de "
						+ fila.sku() + " (" + linea.restante() + ")");
				continue;
			}
			lineas.add(new GestionarOrdenCompra.RecepcionCsvLineaCommand(item.getId(), fila.cantidad(),
					fila.precioUnitario(), fila.fechaVencimiento(), fila.codigoLote()));
		}

		if (!errores.isEmpty()) {
			throw new CsvImportException(errores);
		}
		return gestionarOrdenCompra.registrarRecepcionCsv(
				new GestionarOrdenCompra.RecepcionCsvCommand(ordenId, lineas));
	}
}
