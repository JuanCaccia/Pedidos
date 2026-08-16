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
import com.sistema.stock.port.in.DescartarLote;
import com.sistema.stock.port.in.GestionarMerma;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stock")
@Tag(name = "Stock")
public class StockController {

	private final RegistrarIngreso registrarIngreso;
	private final GestionarMerma gestionarMerma;
	private final AjustarInventario ajustarInventario;
	private final ConsultarStock consultarStock;
	private final DescartarLote descartarLote;

	public StockController(RegistrarIngreso registrarIngreso, GestionarMerma gestionarMerma,
			AjustarInventario ajustarInventario, ConsultarStock consultarStock, DescartarLote descartarLote) {
		this.registrarIngreso = registrarIngreso;
		this.gestionarMerma = gestionarMerma;
		this.ajustarInventario = ajustarInventario;
		this.consultarStock = consultarStock;
		this.descartarLote = descartarLote;
	}

	@PostMapping("/ingresos")
	@Operation(summary = "Registra ingreso de proveedor: crea lote y movimiento INGRESO")
	public ResponseEntity<IngresoResponse> registrarIngreso(@Valid @RequestBody IngresoRequest request) {
		Lote lote = registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				request.itemId(), request.codigoLote(), request.fechaVencimiento(), request.cantidad(), request.motivo(),
				request.proveedorId()));
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
				request.itemId(), request.cantidad(), request.motivo(), request.loteId(), obtenerActorActual()));
		return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoStockResponse.from(movimiento));
	}

	@PostMapping("/lotes/{id}/descartar")
	@Operation(summary = "Descartar lote: lo marca DESCARTADO y registra merma por el saldo disponible (si lo tiene)")
	public ResponseEntity<LoteResponse> descartarLote(@PathVariable Long id) {
		Lote lote = descartarLote.descartar(id);
		return ResponseEntity.ok(LoteResponse.from(lote,
				consultarStock.obtenerDisponibleDeLote(lote.getItemId(), lote.getId()), null, null));
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
	public PageResponse<MovimientoStockResponse> listarMovimientos(@PathVariable Long itemId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		PageResponse<MovimientoStock> pagina = consultarStock.listarMovimientosPaginado(itemId, page, size);
		return PageMapper.of(pagina.content(), pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages(), MovimientoStockResponse::from);
	}

	@GetMapping("/items/{itemId}/lotes")
	public List<LoteResponse> listarLotes(@PathVariable Long itemId) {
		return consultarStock.listarLotes(itemId).stream()
				.map(lote -> LoteResponse.from(lote,
						consultarStock.obtenerDisponibleDeLote(itemId, lote.getId()), null, null))
				.toList();
	}

	@GetMapping("/lotes/por-vencer")
	@Operation(summary = "Lotes con vencimiento dentro de los proximos N dias (incluye vencidos)")
	public List<LoteResponse> lotesPorVencer(@RequestParam(defaultValue = "30") int dias) {
		Map<Long, Item> items = indexarItems();
		return consultarStock.listarLotesPorVencer(dias).stream()
				.map(lote -> LoteResponse.from(lote,
						consultarStock.obtenerDisponibleDeLote(lote.getItemId(), lote.getId()),
						nombreDe(items, lote.getItemId()), skuDe(items, lote.getItemId())))
				.toList();
	}

	@GetMapping("/lotes")
	@Operation(summary = "Lotes con saldo disponible y estado derivado (VENCIDO/AGOTADO/VIGENTE). "
			+ "Filtrable por proveedor con ?proveedorId=")
	public List<LoteResponse> listarTodosLosLotes(@RequestParam(required = false) Long proveedorId) {
		Map<Long, Item> items = indexarItems();
		List<Lote> lotes = proveedorId != null
				? consultarStock.listarLotesPorProveedor(proveedorId)
				: consultarStock.listarTodosLosLotes();
		return lotes.stream()
				.map(lote -> LoteResponse.from(lote,
						consultarStock.obtenerDisponibleDeLote(lote.getItemId(), lote.getId()),
						nombreDe(items, lote.getItemId()), skuDe(items, lote.getItemId())))
				.toList();
	}

	private Map<Long, Item> indexarItems() {
		return consultarStock.listarItems().stream()
				.collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a));
	}

	private String nombreDe(Map<Long, Item> items, Long itemId) {
		Item item = items.get(itemId);
		return item != null ? item.getNombre() : null;
	}

	private String skuDe(Map<Long, Item> items, Long itemId) {
		Item item = items.get(itemId);
		return item != null ? item.getSku() : null;
	}

	private com.sistema.usuario.model.Usuario obtenerActorActual() {
		org.springframework.security.core.Authentication auth =
				org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof com.sistema.usuario.model.Usuario usuario) {
			return usuario;
		}
		return null;
	}
}
