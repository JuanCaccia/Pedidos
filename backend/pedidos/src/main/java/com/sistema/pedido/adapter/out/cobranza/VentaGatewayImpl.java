package com.sistema.pedido.adapter.out.cobranza;

import com.sistema.cobranza.port.out.VentaGateway;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VentaGatewayImpl implements VentaGateway {

	private final ConsultarPedido consultarPedido;

	public VentaGatewayImpl(ConsultarPedido consultarPedido) {
		this.consultarPedido = consultarPedido;
	}

	@Override
	public BigDecimal totalVendidoCliente(Long clienteId) {
		return consultarPedido.listarTodos().stream()
				.filter(p -> p.getClienteId().equals(clienteId))
				.filter(p -> p.getEstado() == EstadoPedido.ENTREGADO
						|| p.getEstado() == EstadoPedido.ENTREGADO_PARCIAL)
				.flatMap(p -> p.getItems().stream())
				.map(i -> i.getCantidadEntregada().multiply(i.getPrecioUnitario()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
