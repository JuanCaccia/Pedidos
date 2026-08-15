package com.sistema.pedido.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.model.PedidoItem;
import com.sistema.pedido.port.in.ConfirmarPedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.pedido.port.in.CrearPedido;
import com.sistema.pedido.port.in.GestionarLogisticaPedido;
import com.sistema.pedido.port.in.GestionarEntrega;
import com.sistema.pedido.port.in.ModificarStockPedido;
import com.sistema.pedido.port.in.ReAgendarPedido;
import com.sistema.pedido.port.in.RechazarPedido;
import com.sistema.pedido.port.out.ClienteGateway;
import com.sistema.pedido.port.out.NotificacionGateway;
import com.sistema.pedido.port.out.PedidoRepository;
import com.sistema.pedido.port.out.RemitoGateway;
import com.sistema.pedido.port.out.StockGateway;
import com.sistema.pedido.port.out.UsuarioGateway;
import com.sistema.usuario.model.Rol;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PedidoService implements CrearPedido, ConfirmarPedido, GestionarEntrega, ReAgendarPedido,
		RechazarPedido, ModificarStockPedido, ConsultarPedido, GestionarLogisticaPedido {

	private final PedidoRepository pedidoRepository;
	private final StockGateway stockGateway;
	private final ClienteGateway clienteGateway;
	private final UsuarioGateway usuarioGateway;
	private final RemitoGateway remitoGateway;
	private final NotificacionGateway notificacionGateway;
	private final ConsultarUsuario consultarUsuario;
	private final long reservaTtlHoras;

	public PedidoService(PedidoRepository pedidoRepository, StockGateway stockGateway,
			ClienteGateway clienteGateway, UsuarioGateway usuarioGateway, RemitoGateway remitoGateway,
			NotificacionGateway notificacionGateway, ConsultarUsuario consultarUsuario,
			@Value("${app.pedido.reserva-ttl-horas:48}") long reservaTtlHoras) {
		this.pedidoRepository = pedidoRepository;
		this.stockGateway = stockGateway;
		this.clienteGateway = clienteGateway;
		this.usuarioGateway = usuarioGateway;
		this.remitoGateway = remitoGateway;
		this.notificacionGateway = notificacionGateway;
		this.consultarUsuario = consultarUsuario;
		this.reservaTtlHoras = reservaTtlHoras;
	}

	@Override
	@Transactional
	public Pedido crearPedido(CrearPedidoCommand command) {
		if (!clienteGateway.existeCliente(command.clienteId())) {
			throw new NotFoundException("Cliente no encontrado: " + command.clienteId());
		}
		if (!usuarioGateway.existeUsuario(command.vendedorId())) {
			throw new NotFoundException("Usuario no encontrado: " + command.vendedorId());
		}
		if (command.items() == null || command.items().isEmpty()) {
			throw new BusinessException("VALIDATION_ERROR", "Un pedido debe tener al menos una línea");
		}
		Pedido pedido = new Pedido(command.clienteId(), command.vendedorId(), command.fechaJornada(),
				command.observaciones(), new ArrayList<>());
		pedido.setFechaCreacion(LocalDateTime.now());
		for (LineaPedidoCommand linea : command.items()) {
			if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
				throw new BusinessException("VALIDATION_ERROR", "La cantidad de la línea debe ser mayor que cero");
			}
			if (linea.precioUnitario() == null || linea.precioUnitario().signum() < 0) {
				throw new BusinessException("VALIDATION_ERROR", "El precio unitario debe ser mayor o igual a cero");
			}
			if (!stockGateway.existeItem(linea.itemId())) {
				throw new NotFoundException("Item no encontrado: " + linea.itemId());
			}
			pedido.agregarItem(new PedidoItem(linea.itemId(), linea.cantidad(), linea.precioUnitario()));
		}
		pedido.setNumero("PED-" + String.format("%06d", System.nanoTime() % 1000000));
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public Pedido confirmarPedido(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		if (pedido.getEstado() != EstadoPedido.PENDIENTE_CONFIRMACION) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "Solo los pedidos en PENDIENTE_CONFIRMACION pueden confirmarse");
		}
		for (PedidoItem item : pedido.getItems()) {
			BigDecimal disponible = stockGateway.consultarDisponible(item.getItemId());
			if (disponible.signum() <= 0) {
				throw new BusinessException("STOCK_INSUFICIENTE",
						"No hay stock disponible para el item " + item.getItemId() + "; el pedido no puede agendarse");
			}
			BigDecimal aReservar = item.getCantidadPedida().min(disponible);
			stockGateway.reservar(item.getItemId(), pedido.getId(), aReservar);
			item.reservar(aReservar);
			if (aReservar.compareTo(item.getCantidadPedida()) < 0) {
				item.marcarPendienteStock();
			}
		}
		boolean stockIncompleto = pedido.getItems().stream().anyMatch(PedidoItem::isPendienteStock);
		pedido.setEstado(stockIncompleto ? EstadoPedido.PENDIENTE_STOCK : EstadoPedido.PENDIENTE_PREPARACION);
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public Pedido registrarEntrega(RegistrarEntregaCommand command) {
		Pedido pedido = obtenerO404(command.pedidoId());
		if (pedido.getEstado() != EstadoPedido.EN_VIAJE) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "Solo los pedidos en EN_VIAJE pueden registrar una entrega");
		}
		Map<Long, BigDecimal> entregas = new HashMap<>();
		for (EntregaLineaCommand linea : command.entregas()) {
			if (linea.cantidadEntregada() == null || linea.cantidadEntregada().signum() < 0) {
				throw new BusinessException("VALIDATION_ERROR", "La cantidad entregada debe ser mayor o igual a cero");
			}
			entregas.put(linea.pedidoItemId(), linea.cantidadEntregada());
		}
		boolean algunEntregado = false;
		for (PedidoItem item : pedido.getItems()) {
			BigDecimal entregado = entregas.getOrDefault(item.getId(), BigDecimal.ZERO);
			if (entregado.compareTo(item.getCantidadReservada()) > 0) {
				throw new BusinessException("ENTREGA_EXCEDE_RESERVA",
						"No se puede entregar más que la cantidad reservada del item " + item.getItemId());
			}
			if (entregado.signum() > 0) {
				stockGateway.egresar(item.getItemId(), pedido.getId(), entregado);
				algunEntregado = true;
			}
			BigDecimal sobrante = item.getCantidadReservada().subtract(entregado);
			if (sobrante.signum() > 0) {
				stockGateway.liberarReserva(item.getItemId(), pedido.getId(), sobrante);
			}
			item.registrarEntrega(entregado);
		}
		if (!algunEntregado) {
			throw new BusinessException("ENTREGA_VACIA", "Debe entregarse al menos una línea");
		}
		List<RemitoGateway.LineaEntregada> lineasEntregadas = pedido.getItems().stream()
				.filter(i -> i.getCantidadEntregada().signum() > 0)
				.map(i -> new RemitoGateway.LineaEntregada(i.getItemId(), i.getCantidadEntregada(), i.getPrecioUnitario()))
				.toList();
		if (!lineasEntregadas.isEmpty()) {
			remitoGateway.generarRemito(pedido.getId(), pedido.getClienteId(), lineasEntregadas);
		}
		boolean entregaTotal = pedido.getItems().stream()
				.allMatch(i -> i.getCantidadEntregada().compareTo(i.getCantidadReservada()) == 0);
		if (entregaTotal) {
			pedido.setEstado(EstadoPedido.ENTREGADO);
		} else {
			pedido.setEstado(EstadoPedido.ENTREGADO_PARCIAL);
			generarPedidoHijo(pedido);
		}
		return pedidoRepository.save(pedido);
	}

	private void generarPedidoHijo(Pedido padre) {
		Pedido hijo = new Pedido(padre.getClienteId(), padre.getVendedorId(), null,
				"Saldo pendiente del pedido " + padre.getNumero(), new ArrayList<>());
		hijo.setPedidoPadreId(padre.getId());
		hijo.setFechaCreacion(LocalDateTime.now());
		for (PedidoItem item : padre.getItems()) {
			BigDecimal saldo = item.getCantidadReservada().subtract(item.getCantidadEntregada());
			if (saldo.signum() > 0) {
				hijo.agregarItem(new PedidoItem(item.getItemId(), saldo, item.getPrecioUnitario()));
			}
		}
		hijo.setNumero("PED-" + String.format("%06d", System.nanoTime() % 1000000));
		pedidoRepository.save(hijo);
	}

	@Override
	@Transactional
	public Pedido reAgendar(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		if (pedido.getEstado() != EstadoPedido.PENDIENTE_ENTREGA
				&& pedido.getEstado() != EstadoPedido.EN_VIAJE) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO",
					"Solo los pedidos en PENDIENTE_ENTREGA o EN_VIAJE pueden re-agendarse");
		}
		pedido.setEstado(EstadoPedido.RE_AGENDADO);
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public void rechazarPedido(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		EstadoPedido estado = pedido.getEstado();
		if (estado != EstadoPedido.PENDIENTE_CONFIRMACION && estado != EstadoPedido.PENDIENTE_STOCK
				&& estado != EstadoPedido.PENDIENTE_PREPARACION && estado != EstadoPedido.PENDIENTE_ENTREGA
				&& estado != EstadoPedido.EN_VIAJE && estado != EstadoPedido.RE_AGENDADO) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "El pedido no puede rechazarse desde el estado " + estado);
		}
		if (estado != EstadoPedido.PENDIENTE_CONFIRMACION) {
			for (PedidoItem item : pedido.getItems()) {
				if (item.getCantidadReservada().signum() > 0) {
					stockGateway.liberarReserva(item.getItemId(), pedido.getId(), item.getCantidadReservada());
				}
			}
		}
		pedido.setEstado(EstadoPedido.RECHAZADO);
		pedidoRepository.save(pedido);
	}

	@Transactional
	public int expirarReservasInactivas() {
		LocalDateTime limite = LocalDateTime.now().minusHours(reservaTtlHoras);
		int expirados = 0;
		for (EstadoPedido estado : List.of(EstadoPedido.PENDIENTE_STOCK,
				EstadoPedido.PENDIENTE_PREPARACION, EstadoPedido.PENDIENTE_ENTREGA,
				EstadoPedido.RE_AGENDADO)) {
			for (Pedido pedido : pedidoRepository.findByEstado(estado)) {
				if (pedido.getUpdatedAt() != null && pedido.getUpdatedAt().isAfter(limite)) {
					continue;
				}
				for (PedidoItem item : pedido.getItems()) {
					if (item.getCantidadReservada().signum() > 0) {
						stockGateway.liberarReserva(item.getItemId(), pedido.getId(), item.getCantidadReservada());
					}
				}
				pedido.setEstado(EstadoPedido.RECHAZADO);
				pedidoRepository.save(pedido);
				expirados++;
			}
		}
		return expirados;
	}

	@Override
	@Transactional
	public Pedido agregarUnidades(Long pedidoId, Long itemId, BigDecimal cantidad) {
		Pedido pedido = obtenerO404(pedidoId);
		EstadoPedido estado = pedido.getEstado();
		if (estado != EstadoPedido.PENDIENTE_STOCK && estado != EstadoPedido.PENDIENTE_PREPARACION
				&& estado != EstadoPedido.PENDIENTE_ENTREGA
				&& estado != EstadoPedido.RE_AGENDADO) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "El pedido no puede modificarse desde el estado " + estado);
		}
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new BusinessException("VALIDATION_ERROR", "La cantidad debe ser mayor que cero");
		}
		PedidoItem item = pedido.itemPorItem(itemId)
				.orElseThrow(() -> new NotFoundException("Línea no encontrada para el item " + itemId));
		if (!item.isPendienteStock()) {
			throw new BusinessException("SIN_PENDIENTE_STOCK", "Esta línea no tiene stock pendiente para agregar");
		}
		BigDecimal pendiente = item.getCantidadPedida().subtract(item.getCantidadReservada());
		if (cantidad.compareTo(pendiente) > 0) {
			throw new BusinessException("VALIDATION_ERROR", "No se puede agregar más que la cantidad pendiente (" + pendiente + ")");
		}
		stockGateway.reservar(itemId, pedido.getId(), cantidad);
		item.agregarStock(cantidad);
		if (pedido.getEstado() == EstadoPedido.PENDIENTE_STOCK
				&& pedido.getItems().stream().noneMatch(PedidoItem::isPendienteStock)) {
			pedido.setEstado(EstadoPedido.PENDIENTE_PREPARACION);
		}
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public Pedido marcarFaltante(MarcarFaltanteCommand command) {
		Pedido pedido = obtenerO404(command.pedidoId());
		if (pedido.getEstado() != EstadoPedido.PENDIENTE_PREPARACION) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "Solo se puede marcar faltante en PENDIENTE_PREPARACION");
		}
		PedidoItem item = pedido.itemPorItem(command.itemId())
				.orElseThrow(() -> new NotFoundException("Línea no encontrada para el item " + command.itemId()));
		if (command.cantidad() == null || command.cantidad().signum() <= 0
				|| command.cantidad().compareTo(item.getCantidadReservada()) > 0) {
			throw new BusinessException("VALIDATION_ERROR", "La cantidad faltante debe ser mayor a cero y no superar lo reservado");
		}
		List<Long> lotes = stockGateway.listarLoteIdsDisponibles(command.itemId());
		if (lotes.isEmpty()) {
			throw new BusinessException("MERMA_SIN_STOCK", "No hay stock físico disponible para marcar el faltante");
		}
		stockGateway.registrarMerma(command.itemId(), lotes.get(0), command.cantidad(), command.motivo());
		item.setCantidadReservada(item.getCantidadReservada().subtract(command.cantidad()));
		item.marcarPendienteStock();
		pedido.setEstado(EstadoPedido.PENDIENTE_STOCK);
		Pedido guardado = pedidoRepository.save(pedido);
		String mensaje = "Diferencia detectada en producto " + command.itemId() + " por "
				+ (command.actor() == null ? "operario" : command.actor().getNombre())
				+ " al armar el pedido " + pedido.getNumero() + " (faltante " + command.cantidad() + ")";
		for (com.sistema.usuario.model.Usuario admin : consultarUsuario.listarTodos()) {
			if (admin.tieneRol(Rol.ADMINISTRATIVO)) {
				notificacionGateway.notificar("FALTANTE_PRODUCTO", mensaje, admin.getId(), pedido.getId());
			}
		}
		return guardado;
	}

	@Override
	@Transactional
	public Pedido consolidarPedidos(ConsolidarCommand command) {
		if (command.pedidoIds() == null || command.pedidoIds().size() < 2) {
			throw new BusinessException("VALIDATION_ERROR", "Se necesitan al menos dos pedidos para consolidar");
		}
		List<Pedido> origen = new ArrayList<>();
		Long clienteId = null;
		Map<Long, PedidoItem> lineasPorItem = new LinkedHashMap<>();
		for (Long pedidoId : command.pedidoIds().stream().distinct().toList()) {
			Pedido pedido = pedidoRepository.findById(pedidoId)
					.orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + pedidoId));
			if (pedido.getEstado() != EstadoPedido.PENDIENTE_CONFIRMACION) {
				throw new BusinessException("PEDIDO_ESTADO_INVALIDO",
						"El pedido " + pedido.getNumero() + " no está en PENDIENTE_CONFIRMACION");
			}
			if (clienteId == null) {
				clienteId = pedido.getClienteId();
			} else if (!clienteId.equals(pedido.getClienteId())) {
				throw new BusinessException("CONSOLIDAR_CLIENTES_DISTINTOS",
						"Los pedidos deben pertenecer al mismo cliente");
			}
			for (PedidoItem item : pedido.getItems()) {
				PedidoItem existente = lineasPorItem.get(item.getItemId());
				if (existente == null) {
					lineasPorItem.put(item.getItemId(),
							new PedidoItem(item.getItemId(), item.getCantidadPedida(), item.getPrecioUnitario()));
				} else {
					if (existente.getPrecioUnitario().compareTo(item.getPrecioUnitario()) != 0) {
						throw new BusinessException("CONSOLIDAR_PRECIOS_DISTINTOS",
								"El item " + item.getItemId() + " tiene precios distintos entre pedidos");
					}
					existente.setCantidadPedida(existente.getCantidadPedida().add(item.getCantidadPedida()));
				}
			}
			origen.add(pedido);
		}
		Pedido consolidado = new Pedido(clienteId, command.vendedorId(), null,
				"Consolidación de " + origen.size() + " pedidos", new ArrayList<>());
		consolidado.setFechaCreacion(LocalDateTime.now());
		lineasPorItem.values().forEach(consolidado::agregarItem);
		consolidado.setNumero("PED-" + String.format("%06d", System.nanoTime() % 1000000));
		Pedido guardado = pedidoRepository.save(consolidado);
		for (Pedido p : origen) {
			p.setEstado(EstadoPedido.RECHAZADO);
			pedidoRepository.save(p);
		}
		return guardado;
	}

	@Override
	public Optional<Pedido> buscarPorId(Long id) {
		return pedidoRepository.findById(id);
	}

	@Override
	public List<Pedido> listarTodos() {
		return pedidoRepository.findAll();
	}

	@Override
	public List<Pedido> listarPorEstado(EstadoPedido estado) {
		return pedidoRepository.findByEstado(estado);
	}

	@Override
	public List<Pedido> listarPorCliente(Long clienteId) {
		return pedidoRepository.findByClienteId(clienteId);
	}

	@Override
	public List<Pedido> listarPorVendedor(Long vendedorId) {
		return pedidoRepository.findByVendedorId(vendedorId);
	}

	@Override
	public List<Pedido> listarHijosDe(Long pedidoPadreId) {
		return pedidoRepository.findByPedidoPadreId(pedidoPadreId);
	}

	@Override
	public Map<EstadoPedido, Long> contadores() {
		Map<EstadoPedido, Long> resultado = new LinkedHashMap<>();
		for (EstadoPedido estado : EstadoPedido.values()) {
			resultado.put(estado, pedidoRepository.contarPorEstado(estado));
		}
		return resultado;
	}

	@Override
	public PageResponse<Pedido> listarPaginado(EstadoPedido estado, Long clienteId, Long vendedorId, int page, int size) {
		List<Pedido> todos;
		if (estado != null) {
			todos = pedidoRepository.findByEstado(estado);
		} else if (clienteId != null) {
			todos = pedidoRepository.findByClienteId(clienteId);
		} else if (vendedorId != null) {
			todos = pedidoRepository.findByVendedorId(vendedorId);
		} else {
			todos = pedidoRepository.findAll();
		}
		return paginar(todos, page, size);
	}

	private <T> PageResponse<T> paginar(List<T> todos, int page, int size) {
		int total = todos.size();
		int from = Math.min(page * size, total);
		int to = Math.min(from + size, total);
		int totalPages = size == 0 ? 0 : (total + size - 1) / size;
		return new PageResponse<>(todos.subList(from, to), page, size, total, totalPages);
	}

	@Override
	@Transactional
	public Pedido asignarARuta(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		EstadoPedido estado = pedido.getEstado();
		if (estado != EstadoPedido.PENDIENTE_ENTREGA && estado != EstadoPedido.RE_AGENDADO) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO",
					"Solo los pedidos en PENDIENTE_ENTREGA o RE_AGENDADO pueden asignarse a una ruta");
		}
		if (estado == EstadoPedido.RE_AGENDADO) {
			pedido.setEstado(EstadoPedido.PENDIENTE_ENTREGA);
		}
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public Pedido despachar(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		if (pedido.getEstado() != EstadoPedido.PENDIENTE_PREPARACION) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO",
					"Solo los pedidos en PENDIENTE_PREPARACION pueden despacharse");
		}
		pedido.setEstado(EstadoPedido.PENDIENTE_ENTREGA);
		return pedidoRepository.save(pedido);
	}

	@Override
	@Transactional
	public Pedido iniciarViaje(Long pedidoId) {
		Pedido pedido = obtenerO404(pedidoId);
		if (pedido.getEstado() != EstadoPedido.PENDIENTE_ENTREGA) {
			throw new BusinessException("PEDIDO_ESTADO_INVALIDO", "Solo los pedidos en PENDIENTE_ENTREGA pueden iniciar un viaje");
		}
		pedido.setEstado(EstadoPedido.EN_VIAJE);
		return pedidoRepository.save(pedido);
	}

	private Pedido obtenerO404(Long pedidoId) {
		return pedidoRepository.findById(pedidoId)
				.orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + pedidoId));
	}
}
