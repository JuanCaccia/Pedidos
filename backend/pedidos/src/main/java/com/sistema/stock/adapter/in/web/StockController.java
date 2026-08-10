package com.sistema.stock.adapter.in.web;

import com.sistema.stock.adapter.in.web.dto.AjusteRequest;
import com.sistema.stock.adapter.in.web.dto.IngresoRequest;
import com.sistema.stock.adapter.in.web.dto.IngresoResponse;
import com.sistema.stock.adapter.in.web.dto.LoteResponse;
import com.sistema.stock.adapter.in.web.dto.MermaRequest;
import com.sistema.stock.adapter.in.web.dto.MovimientoStockResponse;
import com.sistema.stock.adapter.in.web.dto.StockResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.port.in.AjustarInventario;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.stock.port.in.GestionarMerma;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stock")
@Tag(name = "Stock")
public class StockController {

	private final RegistrarIngreso registrarIngreso;
	private final GestionarMerma gestionarMerma;
	private final AjustarInventario ajustarInventario;
	private final ConsultarStock consultarStock;

	public StockController(RegistrarIngreso registrarIngreso, GestionarMerma gestionarMerma,
			AjustarInventario ajustarInventario, ConsultarStock consultarStock) {
		this.registrarIngreso = registrarIngreso;
		this.gestionarMerma = gestionarMerma;
		this.ajustarInventario = ajustarInventario;
		this.consultarStock = consultarStock;
	}

	@PostMapping("/ingresos")
	@Operation(summary = "Registra ingreso de proveedor: crea lote y movimiento INGRESO")
	public ResponseEntity<IngresoResponse> registrarIngreso(@Valid @RequestBody IngresoRequest request) {
		Lote lote = registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				request.itemId(), request.codigoLote(), request.fechaVencimiento(), request.cantidad(), request.motivo()));
		return ResponseEntity.status(HttpStatus.CREATED).body(IngresoResponse.from(lote));
	}

	@PostMapping("/mermas")
	@Operation(summary = "Registra merma vinculada a un lote (solo ENCARGADO_DEPOSITO)")
	public ResponseEntity<MovimientoStockResponse> registrarMerma(@Valid @RequestBody MermaRequest request) {
		MovimientoStock movimiento = gestionarMerma.registrarMerma(new GestionarMerma.RegistrarMermaCommand(
				request.itemId(), request.loteId(), request.cantidad(), request.motivo()));
		return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoStockResponse.from(movimiento));
	}

	@PostMapping("/ajustes")
	@Operation(summary = "Ajuste de inventario firmado (+/-) para corregir diferencias fisicas")
	public ResponseEntity<MovimientoStockResponse> ajustarInventario(@Valid @RequestBody AjusteRequest request) {
		MovimientoStock movimiento = ajustarInventario.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(
				request.itemId(), request.cantidad(), request.motivo()));
		return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoStockResponse.from(movimiento));
	}

	@GetMapping("/items/{itemId}")
	public ResponseEntity<StockResponse> obtenerStock(@PathVariable Long itemId) {
		Item item = consultarStock.buscarItemPorId(itemId)
				.orElseThrow(() -> new NotFoundException("Item no encontrado: " + itemId));
		StockResponse response = StockResponse.of(item,
				consultarStock.obtenerDisponible(itemId),
				consultarStock.obtenerReservasActivas(itemId));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/items/{itemId}/movimientos")
	public List<MovimientoStockResponse> listarMovimientos(@PathVariable Long itemId) {
		return consultarStock.listarMovimientos(itemId).stream().map(MovimientoStockResponse::from).toList();
	}

	@GetMapping("/items/{itemId}/lotes")
	public List<LoteResponse> listarLotes(@PathVariable Long itemId) {
		return consultarStock.listarLotes(itemId).stream().map(LoteResponse::from).toList();
	}

	@GetMapping("/lotes/por-vencer")
	@Operation(summary = "Lotes con vencimiento dentro de los proximos N dias (incluye vencidos)")
	public List<LoteResponse> lotesPorVencer(@RequestParam(defaultValue = "30") int dias) {
		return consultarStock.listarLotesPorVencer(dias).stream().map(LoteResponse::from).toList();
	}
}
