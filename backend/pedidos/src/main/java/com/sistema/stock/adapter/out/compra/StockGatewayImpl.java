package com.sistema.stock.adapter.out.compra;

import com.sistema.compra.port.out.StockGateway;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.service.StockService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
	public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo) {
		registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				itemId, codigoLote, null, cantidad, motivo));
	}
}
