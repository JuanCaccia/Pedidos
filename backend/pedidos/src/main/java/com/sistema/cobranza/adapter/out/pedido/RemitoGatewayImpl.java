package com.sistema.cobranza.adapter.out.pedido;

import com.sistema.cobranza.service.RemitoService;
import com.sistema.pedido.port.out.RemitoGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemitoGatewayImpl implements RemitoGateway {

	private final RemitoService remitoService;

	public RemitoGatewayImpl(RemitoService remitoService) {
		this.remitoService = remitoService;
	}

	@Override
	public long generarRemito(Long pedidoId, Long clienteId, List<LineaEntregada> lineas) {
		List<RemitoService.LineaRemito> convertidas = lineas.stream()
				.map(l -> new RemitoService.LineaRemito(l.itemId(), l.cantidad(), l.precioUnitario()))
				.toList();
		return remitoService.generarRemito(pedidoId, clienteId, convertidas).getId();
	}
}
