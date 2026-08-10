package com.sistema.pedido.adapter.in.web;

import com.sistema.pedido.adapter.in.web.dto.AgregarStockRequest;
import com.sistema.pedido.adapter.in.web.dto.CrearPedidoRequest;
import com.sistema.pedido.adapter.in.web.dto.EntregaRequest;
import com.sistema.pedido.adapter.in.web.dto.LineaRequest;
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
import com.sistema.pedido.port.in.ModificarStockPedido;
import com.sistema.pedido.port.in.ReAgendarPedido;
import com.sistema.pedido.port.in.RechazarPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

	public PedidoController(CrearPedido crearPedido, ConfirmarPedido confirmarPedido,
			GestionarEntrega gestionarEntrega, ReAgendarPedido reAgendarPedido, RechazarPedido rechazarPedido,
			ModificarStockPedido modificarStockPedido, ConsultarPedido consultarPedido) {
		this.crearPedido = crearPedido;
		this.confirmarPedido = confirmarPedido;
		this.gestionarEntrega = gestionarEntrega;
		this.reAgendarPedido = reAgendarPedido;
		this.rechazarPedido = rechazarPedido;
		this.modificarStockPedido = modificarStockPedido;
		this.consultarPedido = consultarPedido;
	}

	@PostMapping
	@Operation(summary = "Crea un pedido en PENDIENTE_CONFIRMACION")
	public ResponseEntity<PedidoResponse> crear(@RequestBody CrearPedidoRequest request) {
		List<LineaPedidoCommand> lineas = request.items().stream()
				.map(l -> new LineaPedidoCommand(l.itemId(), l.cantidad(), l.precioUnitario())).toList();
		Pedido pedido = crearPedido.crearPedido(new CrearPedidoCommand(request.clienteId(), request.vendedorId(),
				request.fechaJornada(), request.observaciones(), lineas));
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
	public ResponseEntity<PedidoResponse> registrarEntrega(@PathVariable Long id, @RequestBody EntregaRequest request) {
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

	@PostMapping("/{id}/rechazar")
	@Operation(summary = "Rechaza el pedido (libera reservas si existian)")
	public ResponseEntity<Void> rechazar(@PathVariable Long id) {
		rechazarPedido.rechazarPedido(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/agregar-stock")
	@Operation(summary = "Agrega unidades al pedido cuando llega stock (cubre pendiente_stock)")
	public ResponseEntity<PedidoResponse> agregarStock(@PathVariable Long id, @RequestBody AgregarStockRequest request) {
		Pedido pedido = modificarStockPedido.agregarUnidades(id, request.itemId(), request.cantidad());
		return ResponseEntity.ok(PedidoResponse.from(pedido));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
		return consultarPedido.buscarPorId(id)
				.map(PedidoResponse::from)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping
	public ResponseEntity<List<PedidoResponse>> listar(@RequestParam(required = false) String estado,
			@RequestParam(required = false) Long clienteId, @RequestParam(required = false) Long vendedorId) {
		List<Pedido> pedidos;
		if (estado != null) {
			try {
				pedidos = consultarPedido.listarPorEstado(EstadoPedido.valueOf(estado));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().build();
			}
		} else if (clienteId != null) {
			pedidos = consultarPedido.listarPorCliente(clienteId);
		} else if (vendedorId != null) {
			pedidos = consultarPedido.listarPorVendedor(vendedorId);
		} else {
			pedidos = consultarPedido.listarTodos();
		}
		return ResponseEntity.ok(pedidos.stream().map(PedidoResponse::from).toList());
	}

	@GetMapping("/{id}/hijos")
	public List<PedidoResponse> listarHijos(@PathVariable Long id) {
		return consultarPedido.listarHijosDe(id).stream().map(PedidoResponse::from).toList();
	}
}
