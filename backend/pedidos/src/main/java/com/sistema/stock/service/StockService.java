package com.sistema.stock.service;

import com.sistema.categoria.port.out.CategoriaRepository;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.Lote;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.model.TipoMovimiento;
import com.sistema.stock.port.in.AjustarInventario;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.stock.port.in.GestionarItem;
import com.sistema.stock.port.in.GestionarMerma;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.port.out.ItemRepository;
import com.sistema.stock.port.out.LoteRepository;
import com.sistema.stock.port.out.MovimientoStockRepository;
import com.sistema.usuario.model.Rol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StockService implements GestionarItem, RegistrarIngreso, GestionarMerma, AjustarInventario, ConsultarStock {

	private final ItemRepository itemRepository;
	private final LoteRepository loteRepository;
	private final MovimientoStockRepository movimientoStockRepository;
	private final CategoriaRepository categoriaRepository;
	private final BigDecimal ajusteMaximoEncargado;

	public StockService(ItemRepository itemRepository, LoteRepository loteRepository,
			MovimientoStockRepository movimientoStockRepository, CategoriaRepository categoriaRepository,
			@Value("${app.stock.ajuste-maximo-encargado:50}") BigDecimal ajusteMaximoEncargado) {
		this.itemRepository = itemRepository;
		this.loteRepository = loteRepository;
		this.movimientoStockRepository = movimientoStockRepository;
		this.categoriaRepository = categoriaRepository;
		this.ajusteMaximoEncargado = ajusteMaximoEncargado;
	}

	// ---------- GestionarItem ----------

	@Override
	@Transactional
	public Item crearItem(CrearItemCommand command) {
		if (command.sku() == null || command.sku().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El SKU es obligatorio");
		}
		if (command.nombre() == null || command.nombre().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre del item es obligatorio");
		}
		if (command.unidadMedida() == null || command.unidadMedida().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "La unidad de medida es obligatoria");
		}
		String sku = command.sku().trim().toUpperCase();
		itemRepository.findBySku(sku).ifPresent(i -> {
			throw new BusinessException("ITEM_SKU_DUPLICADO", "Ya existe un item con ese SKU");
		});
		BigDecimal minimo = command.stockMinimo() == null ? BigDecimal.ZERO : command.stockMinimo();
		if (minimo.signum() < 0) {
			throw new BusinessException("VALIDATION_ERROR", "El stock mínimo debe ser mayor o igual a cero");
		}
		BigDecimal precio = command.precioLista() == null ? BigDecimal.ZERO : command.precioLista();
		if (precio.signum() < 0) {
			throw new BusinessException("VALIDATION_ERROR", "El precio de lista no puede ser negativo");
		}
		Item item = new Item(sku, command.nombre().trim(), command.unidadMedida().trim());
		item.setStockMinimo(minimo);
		item.setPrecioLista(precio);
		asignarCategoria(item, command.categoriaId());
		return resolverNombreCategoria(itemRepository.save(item));
	}

	@Override
	@Transactional
	public Item actualizarItem(ActualizarItemCommand command) {
		Item item = obtenerItemO404(command.itemId());
		if (command.nombre() == null || command.nombre().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "El nombre del item es obligatorio");
		}
		if (command.unidadMedida() == null || command.unidadMedida().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "La unidad de medida es obligatoria");
		}
		BigDecimal minimo = command.stockMinimo() == null ? BigDecimal.ZERO : command.stockMinimo();
		if (minimo.signum() < 0) {
			throw new BusinessException("VALIDATION_ERROR", "El stock mínimo debe ser mayor o igual a cero");
		}
		BigDecimal precio = command.precioLista() == null ? BigDecimal.ZERO : command.precioLista();
		if (precio.signum() < 0) {
			throw new BusinessException("VALIDATION_ERROR", "El precio de lista no puede ser negativo");
		}
		item.setNombre(command.nombre().trim());
		item.setUnidadMedida(command.unidadMedida().trim());
		item.setStockMinimo(minimo);
		item.setPrecioLista(precio);
		asignarCategoria(item, command.categoriaId());
		return resolverNombreCategoria(itemRepository.save(item));
	}

	@Override
	@Transactional
	public void desactivarItem(Long itemId) {
		Item item = obtenerItemO404(itemId);
		item.desactivar();
		itemRepository.save(item);
	}

	@Override
	@Transactional
	public void reactivarItem(Long itemId) {
		Item item = obtenerItemO404(itemId);
		item.setActivo(true);
		itemRepository.save(item);
	}

	// ---------- RegistrarIngreso ----------

	@Override
	@Transactional
	public Lote crearIngreso(CrearIngresoCommand command) {
		Item item = obtenerItemO404(command.itemId());
		validarCantidadPositiva(command.cantidad());
		String codigoLote = command.codigoLote() == null || command.codigoLote().isBlank()
				? "L-" + LocalDate.now() + "-" + (System.nanoTime() % 100000)
				: command.codigoLote().trim();
		String motivo = command.motivo() == null || command.motivo().isBlank()
				? "Ingreso de proveedor"
				: command.motivo().trim();
		Lote lote = new Lote(item.getId(), codigoLote, LocalDate.now(), command.fechaVencimiento(), command.cantidad());
		Lote guardado = loteRepository.save(lote);
		movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.INGRESO, item.getId(), guardado.getId(),
				null, command.cantidad(), LocalDateTime.now(), motivo));
		return guardado;
	}

	// ---------- GestionarMerma ----------
	// NOTE: HTTP role check (ENCARGADO_DEPOSITO only) is enforced in Phase 6 (security).

	@Override
	@Transactional
	public MovimientoStock registrarMerma(RegistrarMermaCommand command) {
		Item item = obtenerItemO404(command.itemId());
		validarItemActivo(item);
		validarCantidadPositiva(command.cantidad());
		Lote lote = loteRepository.findById(command.loteId())
				.orElseThrow(() -> new NotFoundException("Lote no encontrado: " + command.loteId()));
		if (!lote.getItemId().equals(item.getId())) {
			throw new BusinessException("MERMA_LOTE_INCOMPATIBLE", "El lote no pertenece a ese item");
		}
		if (command.motivo() == null || command.motivo().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Se requiere un motivo para la merma");
		}
		if (disponibleDeLote(item.getId(), lote.getId()).compareTo(command.cantidad()) < 0) {
			throw new BusinessException("MERMA_SIN_STOCK",
					"No hay suficiente stock físico en el lote para la merma");
		}
		return movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.MERMA, item.getId(), lote.getId(),
				null, command.cantidad(), LocalDateTime.now(), command.motivo().trim()));
	}

	// ---------- AjustarInventario ----------
	// NOTE: HTTP role check (ENCARGADO_DEPOSITO only) is enforced in Phase 6 (security).

	@Override
	@Transactional
	public MovimientoStock ajustarInventario(AjusteInventarioCommand command) {
		Item item = obtenerItemO404(command.itemId());
		validarItemActivo(item);
		if (command.cantidad() == null || command.cantidad().signum() == 0) {
			throw new BusinessException("VALIDATION_ERROR", "La cantidad del ajuste debe ser distinta de cero");
		}
		if (command.motivo() == null || command.motivo().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Se requiere un motivo para el ajuste de inventario");
		}
		BigDecimal disponible = obtenerDisponible(item.getId());
		if (disponible.add(command.cantidad()).signum() < 0) {
			throw new BusinessException("AJUSTE_SIN_STOCK", "El ajuste dejaría stock negativo");
		}
		if (command.actor() == null || !command.actor().tieneRol(Rol.ADMINISTRATIVO)) {
			if (command.cantidad().abs().compareTo(ajusteMaximoEncargado) > 0) {
				throw new BusinessException("AJUSTE_REQUIERE_ADMIN",
						"El ajuste supera el máximo permitido (" + ajusteMaximoEncargado + "); requiere un usuario ADMINISTRATIVO");
			}
		}
		return movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.AJUSTE_INVENTARIO, item.getId(),
				null, null, command.cantidad(), LocalDateTime.now(), command.motivo().trim()));
	}

	@Transactional
	public List<MovimientoStock> egresarPorLotes(Long itemId, Long pedidoId, BigDecimal cantidad) {
		List<MovimientoStock> creados = new ArrayList<>();
		BigDecimal restante = cantidad;
		List<Lote> lotes = loteRepository.findByItemId(itemId).stream()
				.filter(lote -> lote.getFechaVencimiento() == null
						|| !lote.getFechaVencimiento().isBefore(LocalDate.now()))
				.sorted(Comparator
						.comparing((Lote l) -> l.getFechaVencimiento() == null ? Long.MAX_VALUE : l.getFechaVencimiento().toEpochDay())
						.thenComparing(Lote::getFechaIngreso)
						.thenComparing(Lote::getId))
				.toList();
		for (Lote lote : lotes) {
			if (restante.signum() <= 0) {
				break;
			}
			BigDecimal disponible = disponibleDeLote(itemId, lote.getId());
			if (disponible.signum() <= 0) {
				continue;
			}
			BigDecimal aConsumir = restante.min(disponible);
			MovimientoStock egreso = movimientoStockRepository.save(new MovimientoStock(
					TipoMovimiento.EGRESO_VENTA, itemId, lote.getId(), pedidoId, aConsumir,
					LocalDateTime.now(), "Egreso por venta pedido " + pedidoId));
			creados.add(egreso);
			restante = restante.subtract(aConsumir);
		}
		if (restante.signum() > 0) {
			creados.add(movimientoStockRepository.save(new MovimientoStock(
					TipoMovimiento.EGRESO_VENTA, itemId, null, pedidoId, restante,
					LocalDateTime.now(), "Egreso sin lote pedido " + pedidoId)));
		}
		return creados;
	}

	// ---------- ConsultarStock ----------

	@Override
	public Optional<Item> buscarItemPorId(Long id) {
		return itemRepository.findById(id).map(this::resolverNombreCategoria);
	}

	@Override
	public List<Item> listarItems() {
		return itemRepository.findAll().stream().map(this::resolverNombreCategoria).toList();
	}

	@Override
	public PageResponse<Item> listarItemsPaginado(String q, Long categoriaId, int page, int size) {
		return mapearConCategoria(itemRepository.buscar(q, categoriaId, page, size));
	}

	@Override
	public PageResponse<Item> listarItemsActivosPaginado(String q, Long categoriaId, int page, int size) {
		return mapearConCategoria(itemRepository.buscarActivos(q, categoriaId, page, size));
	}

	@Override
	public List<String> listarCategorias() {
		return categoriaRepository.findByActivoTrue().stream()
				.map(com.sistema.categoria.model.Categoria::getNombre)
				.toList();
	}

	@Override
	public BigDecimal obtenerDisponible(Long itemId) {
		List<MovimientoStock> movimientos = movimientoStockRepository.findByItemIdOrderByFechaAsc(itemId);
		BigDecimal ingresos = sumarPorTipo(movimientos, TipoMovimiento.INGRESO);
		BigDecimal egresos = sumarPorTipo(movimientos, TipoMovimiento.EGRESO_VENTA);
		BigDecimal mermas = sumarPorTipo(movimientos, TipoMovimiento.MERMA);
		BigDecimal ajustes = movimientos.stream()
				.filter(m -> m.getTipo() == TipoMovimiento.AJUSTE_INVENTARIO)
				.map(MovimientoStock::getCantidad)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal reservasActivas = obtenerReservasActivas(movimientos, egresos);
		// Formula de negocio: Disponible = Ingresos - Reservas Activas - Egresos - Mermas (+/- ajuste).
		// Como el EGRESO_VENTA cierra la reserva que consumio, Reservas Activas = RESERVA - LIBERACION - EGRESO
		// y el EGRESO se cancela algebraicamente: el resultado es consistente con la fisica del deposito.
		return ingresos.add(ajustes).subtract(reservasActivas).subtract(egresos).subtract(mermas);
	}

	@Override
	public BigDecimal obtenerReservasActivas(Long itemId) {
		List<MovimientoStock> movimientos = movimientoStockRepository.findByItemIdOrderByFechaAsc(itemId);
		BigDecimal egresos = sumarPorTipo(movimientos, TipoMovimiento.EGRESO_VENTA);
		return obtenerReservasActivas(movimientos, egresos);
	}

	private BigDecimal obtenerReservasActivas(List<MovimientoStock> movimientos, BigDecimal egresos) {
		BigDecimal reservas = sumarPorTipo(movimientos, TipoMovimiento.RESERVA_PEDIDO);
		BigDecimal liberaciones = sumarPorTipo(movimientos, TipoMovimiento.LIBERACION_RESERVA);
		return reservas.subtract(liberaciones).subtract(egresos);
	}

	private BigDecimal sumarPorTipo(List<MovimientoStock> movimientos, TipoMovimiento tipo) {
		return movimientos.stream()
				.filter(m -> m.getTipo() == tipo)
				.map(MovimientoStock::getCantidad)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal disponibleDeLote(Long itemId, Long loteId) {
		return obtenerDisponibleDeLote(itemId, loteId);
	}

	@Override
	public BigDecimal obtenerDisponibleDeLote(Long itemId, Long loteId) {
		List<MovimientoStock> movimientos = movimientoStockRepository.findByItemIdOrderByFechaAsc(itemId);
		BigDecimal ingresos = movimientos.stream()
				.filter(m -> m.getTipo() == TipoMovimiento.INGRESO && loteId.equals(m.getLoteId()))
				.map(MovimientoStock::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal egresos = movimientos.stream()
				.filter(m -> m.getTipo() == TipoMovimiento.EGRESO_VENTA && loteId.equals(m.getLoteId()))
				.map(MovimientoStock::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal mermas = movimientos.stream()
				.filter(m -> m.getTipo() == TipoMovimiento.MERMA && loteId.equals(m.getLoteId()))
				.map(MovimientoStock::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
		return ingresos.subtract(egresos).subtract(mermas);
	}

	@Override
	public List<MovimientoStock> listarMovimientos(Long itemId) {
		return movimientoStockRepository.findByItemIdOrderByFechaAsc(itemId);
	}

	@Override
	public PageResponse<MovimientoStock> listarMovimientosPaginado(Long itemId, int page, int size) {
		return movimientoStockRepository.listarPaginado(itemId, page, size);
	}

	@Override
	public List<Lote> listarLotes(Long itemId) {
		return loteRepository.findByItemId(itemId);
	}

	@Override
	public List<Lote> listarLotesPorVencer(int dias) {
		return loteRepository.findByFechaVencimientoNotNullAndFechaVencimientoLessThanEqual(LocalDate.now().plusDays(dias));
	}

	@Override
	public List<Lote> listarTodosLosLotes() {
		return loteRepository.findAll();
	}

	private Item obtenerItemO404(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Item no encontrado: " + itemId));
	}

	private void validarCantidadPositiva(BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new BusinessException("VALIDATION_ERROR", "La cantidad debe ser mayor que cero");
		}
	}

	private void validarItemActivo(Item item) {
		if (!item.isActivo()) {
			throw new BusinessException("ITEM_INACTIVO", "El item " + item.getId() + " está inactivo y no admite operaciones de stock");
		}
	}

	private void asignarCategoria(Item item, Long categoriaId) {
		item.setCategoriaId(categoriaId);
		if (categoriaId != null) {
			resolverNombreCategoria(item);
			if (item.getCategoriaNombre() == null) {
				throw new BusinessException("CATEGORIA_NO_ENCONTRADA", "La categoría no existe: " + categoriaId);
			}
		} else {
			item.setCategoriaNombre(null);
		}
	}

	private Item resolverNombreCategoria(Item item) {
		if (item.getCategoriaId() != null && item.getCategoriaNombre() == null) {
			categoriaRepository.findById(item.getCategoriaId())
					.ifPresent(c -> item.setCategoriaNombre(c.getNombre()));
		}
		return item;
	}

	private PageResponse<Item> mapearConCategoria(PageResponse<Item> pagina) {
		return new PageResponse<>(pagina.content().stream().map(this::resolverNombreCategoria).toList(),
				pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
	}
}
