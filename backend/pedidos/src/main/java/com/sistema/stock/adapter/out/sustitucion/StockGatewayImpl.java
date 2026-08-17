package com.sistema.stock.adapter.out.sustitucion;

import com.sistema.sustitucion.port.out.StockGateway;
import com.sistema.stock.port.in.RegistrarIngreso;
import com.sistema.stock.service.StockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component("stockGatewaySustitucion")
public class StockGatewayImpl implements StockGateway {

	private final StockService stockService;
	private final RegistrarIngreso registrarIngreso;

	public StockGatewayImpl(StockService stockService, RegistrarIngreso registrarIngreso) {
		this.stockService = stockService;
		this.registrarIngreso = registrarIngreso;
	}

	@Override
	@Transactional
	public void registrarIngreso(Long itemId, String codigoLote, BigDecimal cantidad, String motivo) {
		registrarIngreso.crearIngreso(new RegistrarIngreso.CrearIngresoCommand(
				itemId, codigoLote, null, cantidad, motivo, null, null));
	}

	@Override
	@Transactional
	public void egresar(Long itemId, Long pedidoId, BigDecimal cantidad) {
		stockService.egresarPorLotes(itemId, pedidoId, cantidad);
	}

	@Override
	public BigDecimal consultarPrecioLista(Long itemId) {
		return stockService.buscarItemPorId(itemId)
				.map(com.sistema.stock.model.Item::getPrecioLista)
				.orElse(BigDecimal.ZERO);
	}
}
