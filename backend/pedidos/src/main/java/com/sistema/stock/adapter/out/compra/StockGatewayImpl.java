package com.sistema.stock.adapter.out.compra;

import com.sistema.compra.port.out.StockGateway;
import com.sistema.stock.model.Lote;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.service.StockService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component("stockGatewayCompra")
public class StockGatewayImpl implements StockGateway {

	private final StockService stockService;
	private final RegistrarIngreso registrarIngreso;

	public StockGatewayImpl(StockService stockService, RegistrarIngreso registrarIngreso) {
		this.stockService = stockService;
		this.registrarIngreso = registrarIngreso;
	}

	@Override
	public boolean existeItem(Long itemId) {
		return stockService.buscarItemPorId(itemId).isPresent();
	}

	@Override
	public boolean itemActivo(Long itemId) {
		return stockService.buscarItemPorId(itemId).map(com.sistema.stock.model.Item::isActivo).orElse(false);
	}

	@Override
	public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo, Long proveedorId,
			BigDecimal precioUnitario) {
		registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				itemId, codigoLote, null, cantidad, motivo, proveedorId, precioUnitario));
	}

	@Override
	public Lote registrarIngresoConLote(Long itemId, String codigoLote, LocalDate fechaVencimiento,
			BigDecimal cantidad, String motivo, Long proveedorId, BigDecimal precioUnitario) {
		return registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				itemId, codigoLote, fechaVencimiento, cantidad, motivo, proveedorId, precioUnitario));
	}
}
