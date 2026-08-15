package com.sistema.pedido.adapter.in.web;

import com.sistema.pedido.adapter.in.web.dto.AgregarStockRequest;
import com.sistema.pedido.adapter.in.web.dto.ConsolidarRequest;
import com.sistema.pedido.adapter.in.web.dto.CrearPedidoRequest;
import com.sistema.pedido.adapter.in.web.dto.EntregaRequest;
import com.sistema.pedido.adapter.in.web.dto.LineaRequest;
import com.sistema.pedido.adapter.in.web.dto.MarcarFaltanteRequest;
import com.sistema.pedido.adapter.in.web.dto.PedidoResponse;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConfirmarPedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.pedido.port.in.CrearPedido;
import com.sistema.pedido.port.in.CrearPedido.CrearPedidoCommand;
import com.sistema.pedido.port.in.CrearPedido.LineaPedidoCommand;
import com.sistema.pedido.port.in.GestionarEntrega;
import com.sistema.pedido.port.in.GestionarEntrega.EntregaLineaCommand;
import com.sistema.pedido.port.in.GestionarEntrega.RegistrarEntregaCommand;
import com.sistema.pedido.port.in.GestionarLogisticaPedido;
import com.sistema.pedido.port.in.ModificarStockPedido;
import com.sistema.pedido.port.in.ReAgendarPedido;
import com.sistema.pedido.port.in.RechazarPedido;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.common.util.CsvWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

	private final CrearPedido crearPedido;
	private final ConfirmarPedido confirmarPedido;
	private final GestionarEntrega gestionarEntrega;
	private final ReAgendarPedido reAgendarPedido;
	private final RechazarPedido rechazarPedido;
	private final ModificarStockPedido modificarStockPedido;
	private final ConsultarPedido consultarPedido;
	private final GestionarLogisticaPedido gestionarLogisticaPedido;

	public PedidoController(CrearPedido crearPedido, ConfirmarPedido confirmarPedido,
			GestionarEntrega gestionarEntrega, ReAgendarPedido reAgendarPedido, RechazarPedido rechazarPedido,
			ModificarStockPedido modificarStockPedido, ConsultarPedido consultarPedido,
			GestionarLogisticaPedido gestionarLogisticaPedido) {
		this.crearPedido = crearPedido;
		this.confirmarPedido = confirmarPedido;
		this.gestionarEntrega = gestionarEntrega;
		this.reAgendarPedido = reAgendarPedido;
		this.rechazarPedido = rechazarPedido;
		this.modificarStockPedido = modificarStockPedido;
		this.consultarPedido = consultarPedido;
		this.gestionarLogisticaPedido = gestionarLogisticaPedido;
	}

	@PostMapping
	@Operation(summary = "Crea un pedido en PENDIENTE_CONFIRMACION")
	public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest request) {
		List<LineaPedidoCommand> lineas = request.items().stream()
				.map(l -> new LineaPedidoCommand(l.itemId(), l.cantidad(), l.precioUnitario())).toList();
		Pedido pedido = crearPedido.crearPedido(new CrearPedidoCommand(request.clienteId(), request.vendedorId(),
				request.fechaJornada(), request.observaciones(), Boolean.TRUE.equals(request.express()), lineas));
		return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.from(pedido));
	}

	@PostMapping("/{id}/confirmar")
	@Operation(summary = "Confirma el pedido y reserva stock disponible (bloquea si no hay stock)")
	public ResponseEntity<PedidoResponse> confirmar(@PathVariable Long id) {
		Pedido pedido = confirmarPedido.confirmarPedido(id);
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@PostMapping("/{id}/entregas")
	@Operation(summary = "Registra entrega total o parcial; el parcial cierra el pedido y genera un pedido hijo")
	public ResponseEntity<PedidoResponse> registrarEntrega(@PathVariable Long id, @Valid @RequestBody EntregaRequest request) {
		List<EntregaLineaCommand> lineas = request.entregas().stream()
				.map(e -> new EntregaLineaCommand(e.pedidoItemId(), e.cantidadEntregada())).toList();
		Pedido pedido = gestionarEntrega.registrarEntrega(new RegistrarEntregaCommand(id, lineas));
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@PostMapping("/{id}/reagendar")
	@Operation(summary = "Re-agenda el pedido manteniendo la reserva activa")
	public ResponseEntity<PedidoResponse> reagendar(@PathVariable Long id) {
		Pedido pedido = reAgendarPedido.reAgendar(id);
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@PostMapping("/{id}/despachar")
	@Operation(summary = "Despacha el pedido desde el deposito (PENDIENTE_PREPARACION -> PENDIENTE_ENTREGA)")
	public ResponseEntity<PedidoResponse> despachar(@PathVariable Long id) {
		return ResponseEntity.ok(PedidoResponse.from(gestionarLogisticaPedido.despachar(id)));
	}

	@PostMapping("/{id}/rechazar")
	@Operation(summary = "Rechaza el pedido (libera reservas si existian)")
	public ResponseEntity<Void> rechazar(@PathVariable Long id) {
		rechazarPedido.rechazarPedido(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/agregar-stock")
	@Operation(summary = "Agrega unidades al pedido cuando llega stock (cubre pendiente_stock)")
	public ResponseEntity<PedidoResponse> agregarStock(@PathVariable Long id, @Valid @RequestBody AgregarStockRequest request) {
		Pedido pedido = modificarStockPedido.agregarUnidades(id, request.itemId(), request.cantidad());
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@PostMapping("/{id}/marcar-faltante")
	@Operation(summary = "Marca un faltante/danado de una linea; registra merma y deja el pedido en backorder (PENDIENTE_STOCK)")
	public ResponseEntity<PedidoResponse> marcarFaltante(@PathVariable Long id,
			@Valid @RequestBody MarcarFaltanteRequest request) {
		Pedido pedido = modificarStockPedido.marcarFaltante(new ModificarStockPedido.MarcarFaltanteCommand(id,
				request.itemId(), request.cantidad(), request.motivo(), obtenerActorActual()));
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@PostMapping("/consolidar")
	@Operation(summary = "Consolida varios pedidos PENDIENTE_CONFIRMACION del mismo cliente en uno y cancela los origenes")
	public ResponseEntity<PedidoResponse> consolidar(@Valid @RequestBody ConsolidarRequest request) {
		Pedido pedido = modificarStockPedido.consolidarPedidos(
				new ModificarStockPedido.ConsolidarCommand(request.pedidoIds(), obtenerActorActual().getId()));
		return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.from(pedido));
	}

	@GetMapping("/contadores")
	public Map<EstadoPedido, Long> contadores() {
		return consultarPedido.contadores();
	}

	@GetMapping("/exportar.csv")
	@Operation(summary = "Exporta los pedidos a CSV")
	public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) String estado) {
		List<Pedido> pedidos = estado == null ? consultarPedido.listarTodos()
				: consultarPedido.listarPorEstado(EstadoPedido.valueOf(estado));
		List<String> headers = List.of("numero", "clienteId", "estado", "total", "fechaCreacion");
		List<List<String>> filas = new ArrayList<>();
		for (Pedido pedido : pedidos) {
			filas.add(List.of(pedido.getNumero(), String.valueOf(pedido.getClienteId()),
					pedido.getEstado().name(),
					pedido.getTotal() == null ? "" : pedido.getTotal().toPlainString(),
					pedido.getFechaCreacion() == null ? "" : String.valueOf(pedido.getFechaCreacion())));
		}
		String csv = CsvWriter.escribir(headers, filas);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
		responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pedidos.csv\"");
		return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), responseHeaders, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
		Pedido pedido = consultarPedido.buscarPorId(id)
				.orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + id));
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@GetMapping
	public ResponseEntity<PageResponse<PedidoResponse>> listar(@RequestParam(required = false) String estado,
			@RequestParam(required = false) Long clienteId, @RequestParam(required = false) Long vendedorId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		EstadoPedido estadoEnum = null;
		if (estado != null) {
			try {
				estadoEnum = EstadoPedido.valueOf(estado);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().build();
			}
		}
		PageResponse<Pedido> pagina = consultarPedido.listarPaginado(estadoEnum, clienteId, vendedorId, page, size);
		PageResponse<PedidoResponse> respuesta = new PageResponse<>(
				pagina.content().stream().map(PedidoResponse::from).toList(),
				pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
		return ResponseEntity.ok(respuesta);
	}

	@GetMapping("/{id}/hijos")
	public List<PedidoResponse> listarHijos(@PathVariable Long id) {
		return consultarPedido.listarHijosDe(id).stream().map(PedidoResponse::from).toList();
	}

	private com.sistema.usuario.model.Usuario obtenerActorActual() {
		org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof com.sistema.usuario.model.Usuario usuario) {
			return usuario;
		}
		throw new BusinessException("AUTH_INVALIDO", "No autenticado");
	}
}
