package com.sistema.stock.adapter.out.pedido;

import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.pedido.port.out.StockGateway;
import com.sistema.stock.adapter.out.persistence.ItemJpaRepository;
import com.sistema.stock.model.MovimientoStock;
import com.sistema.stock.model.TipoMovimiento;
import com.sistema.stock.port.out.MovimientoStockRepository;
import com.sistema.stock.service.StockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class StockGatewayImpl implements StockGateway {

	private final StockService stockService;
	private final ItemJpaRepository itemJpaRepository;
	private final MovimientoStockRepository movimientoStockRepository;

	public StockGatewayImpl(StockService stockService, ItemJpaRepository itemJpaRepository,
			MovimientoStockRepository movimientoStockRepository) {
		this.stockService = stockService;
		this.itemJpaRepository = itemJpaRepository;
		this.movimientoStockRepository = movimientoStockRepository;
	}

	@Override
	public boolean existeItem(Long itemId) {
		return stockService.buscarItemPorId(itemId).isPresent();
	}

	@Override
	public BigDecimal consultarDisponible(Long itemId) {
		return stockService.obtenerDisponible(itemId);
	}

	@Override
	@Transactional
	public void reservar(Long itemId, Long pedidoId, BigDecimal cantidad) {
		// Bloqueo pesimista: serializa reservas concurrentes sobre el mismo item
		itemJpaRepository.findByIdParaActualizar(itemId)
				.orElseThrow(() -> new NotFoundException("Item no encontrado: " + itemId));
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
		movimientoStockRepository.save(new MovimientoStock(TipoMovimiento.EGRESO_VENTA, itemId, null, pedidoId,
				cantidad, LocalDateTime.now(), "Egreso por venta del pedido " + pedidoId));
	}
}
