package com.sistema.stock.adapter.out.pedido;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.pedido.port.out.StockGateway;
import com.sistema.stock.adapter.out.persistence.ItemJpaEntity;
import com.sistema.stock.adapter.out.persistence.ItemJpaRepository;
import com.sistema.stock.model.Item;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.model.TipoMovimiento;
import com.sistema.stock.port.in.GestionarMerma;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.port.out.MovimientoStockRepository;
import com.sistema.stock.service.StockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class StockGatewayImpl implements StockGateway {

	private final StockService stockService;
	private final ItemJpaRepository itemJpaRepository;
	private final MovimientoStockRepository movimientoStockRepository;
	private final RegistrarIngreso registrarIngreso;

	public StockGatewayImpl(StockService stockService, ItemJpaRepository itemJpaRepository,
			MovimientoStockRepository movimientoStockRepository, RegistrarIngreso registrarIngreso) {
		this.stockService = stockService;
		this.itemJpaRepository = itemJpaRepository;
		this.movimientoStockRepository = movimientoStockRepository;
		this.registrarIngreso = registrarIngreso;
	}

	@Override
	public boolean existeItem(Long itemId) {
		return stockService.buscarItemPorId(itemId).isPresent();
	}

	@Override
	public boolean itemActivo(Long itemId) {
		return stockService.buscarItemPorId(itemId).map(Item::isActivo).orElse(false);
	}

	@Override
	public BigDecimal consultarDisponible(Long itemId) {
		return stockService.obtenerDisponible(itemId);
	}

	@Override
	@Transactional
	public void reservar(Long itemId, Long pedidoId, BigDecimal cantidad) {
		// Bloqueo pesimista: serializa reservas concurrentes sobre el mismo item
		ItemJpaEntity item = itemJpaRepository.findByIdParaActualizar(itemId)
				.orElseThrow(() -> new NotFoundException("Item no encontrado: " + itemId));
		if (!item.isActivo()) {
			throw new BusinessException("ITEM_INACTIVO", "El item " + itemId + " está inactivo y no puede reservarse");
		}
		BigDecimal disponible = stockService.obtenerDisponible(itemId);
		if (disponible.compareTo(cantidad) < 0) {
			throw new BusinessException("STOCK_INSUFICIENTE",
					"No hay stock disponible suficiente para reservar " + cantidad + " del item " + itemId);
		}
		movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.RESERVA_PEDIDO, itemId, null, pedidoId,
				cantidad, LocalDateTime.now(), "Reserva por pedido " + pedidoId));
	}

	@Override
	@Transactional
	public void liberarReserva(Long itemId, Long pedidoId, BigDecimal cantidad) {
		movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.LIBERACION_RESERVA, itemId, null, pedidoId,
				cantidad, LocalDateTime.now(), "Liberación de reserva del pedido " + pedidoId));
	}

	@Override
	@Transactional
	public void egresar(Long itemId, Long pedidoId, BigDecimal cantidad) {
		stockService.egresarPorLotes(itemId, pedidoId, cantidad);
	}

	@Override
	public List<Long> listarLoteIdsDisponibles(Long itemId) {
		return stockService.listarLotes(itemId).stream()
				.filter(lote -> stockService.obtenerDisponibleDeLote(itemId, lote.getId()).signum() > 0)
				.map(com.sistema.stock.model.Lote::getId)
				.toList();
	}

	@Override
	@Transactional
	public void registrarMerma(Long itemId, Long loteId, BigDecimal cantidad, String motivo) {
		stockService.registrarMerma(new GestionarMerma.RegistrarMermaCommand(itemId, loteId, cantidad, motivo));
	}

	@Override
	@Transactional
	public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo) {
		registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				itemId, codigoLote, null, cantidad, motivo));
	}

	@Override
	public BigDecimal consultarPrecioLista(Long itemId) {
		return stockService.buscarItemPorId(itemId)
				.map(Item::getPrecioLista)
				.orElse(BigDecimal.ZERO);
	}
}
