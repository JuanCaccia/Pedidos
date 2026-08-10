package com.sistema.stock.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StockService implements GestionarItem, RegistrarIngreso, GestionarMerma, AjustarInventario, ConsultarStock {

	private final ItemRepository itemRepository;
	private final LoteRepository loteRepository;
	private final MovimientoStockRepository movimientoStockRepository;

	public StockService(ItemRepository itemRepository, LoteRepository loteRepository,
			MovimientoStockRepository movimientoStockRepository) {
		this.itemRepository = itemRepository;
		this.loteRepository = loteRepository;
		this.movimientoStockRepository = movimientoStockRepository;
	}

	// ---------- GestionarItem ----------

	@Override
	@Transactional
	public Item crearItem(CrearItemCommand command) {
		if (command.sku() == null || command.sku().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "SKU is required");
		}
		if (command.nombre() == null || command.nombre().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Item name is required");
		}
		if (command.unidadMedida() == null || command.unidadMedida().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "Unit of measure is required");
		}
		String sku = command.sku().trim().toUpperCase();
		itemRepository.findBySku(sku).ifPresent(i -> {
			throw new BusinessException("ITEM_SKU_DUPLICADO", "An item with that SKU already exists");
		});
		return itemRepository.save(new Item(sku, command.nombre().trim(), command.unidadMedida().trim()));
	}

	@Override
	@Transactional
	public void desactivarItem(Long itemId) {
		Item item = obtenerItemO404(itemId);
		item.desactivar();
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
		validarCantidadPositiva(command.cantidad());
		Lote lote = loteRepository.findById(command.loteId())
				.orElseThrow(() -> new NotFoundException("Batch not found: " + command.loteId()));
		if (!lote.getItemId().equals(item.getId())) {
			throw new BusinessException("MERMA_LOTE_INCOMPATIBLE", "The batch does not belong to that item");
		}
		if (command.motivo() == null || command.motivo().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "A reason is required for merma");
		}
		if (obtenerDisponible(item.getId()).compareTo(command.cantidad()) < 0) {
			throw new BusinessException("MERMA_SIN_STOCK", "Not enough available stock for the merma");
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
		if (command.cantidad() == null || command.cantidad().signum() == 0) {
			throw new BusinessException("VALIDATION_ERROR", "Adjustment quantity must be non-zero");
		}
		if (command.motivo() == null || command.motivo().isBlank()) {
			throw new BusinessException("VALIDATION_ERROR", "A reason is required for inventory adjustment");
		}
		BigDecimal disponible = obtenerDisponible(item.getId());
		if (disponible.add(command.cantidad()).signum() < 0) {
			throw new BusinessException("AJUSTE_SIN_STOCK", "Adjustment would leave negative stock");
		}
		return movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.AJUSTE_INVENTARIO, item.getId(),
				null, null, command.cantidad(), LocalDateTime.now(), command.motivo().trim()));
	}

	// ---------- ConsultarStock ----------

	@Override
	public Optional<Item> buscarItemPorId(Long id) {
		return itemRepository.findById(id);
	}

	@Override
	public List<Item> listarItems() {
		return itemRepository.findAll();
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

	@Override
	public List<MovimientoStock> listarMovimientos(Long itemId) {
		return movimientoStockRepository.findByItemIdOrderByFechaAsc(itemId);
	}

	@Override
	public List<Lote> listarLotes(Long itemId) {
		return loteRepository.findByItemId(itemId);
	}

	private Item obtenerItemO404(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
	}

	private void validarCantidadPositiva(BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new BusinessException("VALIDATION_ERROR", "Quantity must be greater than zero");
		}
	}
}
