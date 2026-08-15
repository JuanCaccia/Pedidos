package com.sistema.pedido.adapter.out.ruta;

import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.pedido.port.in.GestionarLogisticaPedido;
import com.sistema.pedido.port.out.ClienteGateway;
import com.sistema.ruta.port.out.PedidoGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PedidoGatewayImpl implements PedidoGateway {

	private final ConsultarPedido consultarPedido;
	private final GestionarLogisticaPedido gestionarLogisticaPedido;
	private final ClienteGateway clienteGateway;

	public PedidoGatewayImpl(ConsultarPedido consultarPedido, GestionarLogisticaPedido gestionarLogisticaPedido,
			ClienteGateway clienteGateway) {
		this.consultarPedido = consultarPedido;
		this.gestionarLogisticaPedido = gestionarLogisticaPedido;
		this.clienteGateway = clienteGateway;
	}

	@Override
	public BigDecimal unidadesDe(Long pedidoId) {
		return consultarPedido.buscarPorId(pedidoId)
				.map(p -> p.getItems().stream()
						.map(com.sistema.pedido.model.PedidoItem::getCantidadReservada)
						.reduce(BigDecimal.ZERO, BigDecimal::add))
				.orElse(BigDecimal.ZERO);
	}

	@Override
	public String numeroDePedido(Long pedidoId) {
		return consultarPedido.buscarPorId(pedidoId)
				.map(Pedido::getNumero)
				.orElse("PED-" + pedidoId);
	}

	@Override
	public boolean existePedido(Long pedidoId) {
		return consultarPedido.buscarPorId(pedidoId).isPresent();
	}

	@Override
	public boolean estaDisponibleParaRuta(Long pedidoId) {
		return consultarPedido.buscarPorId(pedidoId)
				.map(p -> p.getEstado() == EstadoPedido.PENDIENTE_ENTREGA
						|| p.getEstado() == EstadoPedido.RE_AGENDADO)
				.orElse(false);
	}

	@Override
	public boolean clientePerteneceAZona(Long pedidoId, Long zonaId) {
		Optional<Pedido> pedido = consultarPedido.buscarPorId(pedidoId);
		if (pedido.isEmpty()) {
			return false;
		}
		return clienteGateway.zonaDeCliente(pedido.get().getClienteId())
				.map(z -> z.equals(zonaId))
				.orElse(false);
	}

	@Override
	public void asignarARuta(Long pedidoId) {
		gestionarLogisticaPedido.asignarARuta(pedidoId);
	}

	@Override
	public void iniciarViaje(Long pedidoId) {
		gestionarLogisticaPedido.iniciarViaje(pedidoId);
	}
}
