package com.sistema.sustitucion.service;

import com.sistema.cobranza.model.FormaPago;
import com.sistema.cobranza.port.in.RegistrarCobranza;
import com.sistema.common.exception.BusinessException;
import com.sistema.common.exception.NotFoundException;
import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.model.PedidoItem;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.stock.port.in.AjustarInventario;
import com.sistema.sustitucion.model.Sustitucion;
import com.sistema.sustitucion.port.in.RegistrarSustitucion;
import com.sistema.sustitucion.port.out.StockGateway;
import com.sistema.sustitucion.port.out.SustitucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SustitucionService implements RegistrarSustitucion {

	private final SustitucionRepository sustitucionRepository;
	private final StockGateway stockGateway;
	private final ConsultarPedido consultarPedido;
	private final RegistrarCobranza registrarCobranza;
	private final AjustarInventario ajustarInventario;

	public SustitucionService(SustitucionRepository sustitucionRepository, StockGateway stockGateway,
			ConsultarPedido consultarPedido, RegistrarCobranza registrarCobranza, AjustarInventario ajustarInventario) {
		this.sustitucionRepository = sustitucionRepository;
		this.stockGateway = stockGateway;
		this.consultarPedido = consultarPedido;
		this.registrarCobranza = registrarCobranza;
		this.ajustarInventario = ajustarInventario;
	}

	@Override
	@Transactional
	public Sustitucion sustituir(SustituirCommand command) {
		Pedido pedido = consultarPedido.buscarPorId(command.pedidoId())
				.orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + command.pedidoId()));

			if (pedido.getEstado() != EstadoPedido.ENTREGADO && pedido.getEstado() != EstadoPedido.ENTREGADO_PARCIAL
					&& pedido.getEstado() != EstadoPedido.EN_VIAJE) {
				throw new BusinessException("PEDIDO_ESTADO_INVALIDO",
						"Solo se pueden sustituir ítems de pedidos ENTREGADO, ENTREGADO_PARCIAL o EN_VIAJE");
			}
		if (command.cantidad() == null || command.cantidad().signum() <= 0) {
			throw new BusinessException("VALIDATION_ERROR", "La cantidad debe ser mayor que cero");
		}
		if (command.itemOriginalId().equals(command.itemSustitutoId())) {
			throw new BusinessException("MISMO_ITEM", "El ítem original y el sustituto no pueden ser el mismo");
		}
		PedidoItem itemOriginal = pedido.itemPorItem(command.itemOriginalId())
				.orElseThrow(() -> new BusinessException("ITEM_NO_PERTENECE_AL_PEDIDO",
						"El ítem " + command.itemOriginalId() + " no pertenece al pedido " + command.pedidoId()));

		if (pedido.getEstado() == EstadoPedido.EN_VIAJE) {
			if (command.cantidad().compareTo(itemOriginal.getCantidadReservada()) > 0) {
				throw new BusinessException("CANTIDAD_EXCEDE_RESERVA",
						"La cantidad a sustituir no puede exceder la cantidad reservada del ítem "
								+ command.itemOriginalId());
			}
			// En EN_VIAJE el ítem original solo está reservado (no egresado): se egresa
			// primero (sale físicamente) y recién luego se registra el ingreso, para no
			// devolver a stock unidades que nunca salieron.
			stockGateway.egresar(command.itemOriginalId(), command.pedidoId(), command.cantidad());
		}

		BigDecimal precioOriginal = stockGateway.consultarPrecioLista(command.itemOriginalId());
		BigDecimal precioSustituto = stockGateway.consultarPrecioLista(command.itemSustitutoId());
		BigDecimal diferenciaPrecio = precioOriginal.subtract(precioSustituto).multiply(command.cantidad());

		stockGateway.registrarIngreso(command.itemOriginalId(), "SUST-" + command.pedidoId(), command.cantidad(),
				"Devolución por sustitución en pedido " + command.pedidoId());
		// El ítem sustituto salió físicamente sin reserva previa: se descuenta con un
		// ajuste negativo, porque la fórmula de disponible cancela los EGRESO (asumen
		// que cierran una reserva).
		ajustarInventario.ajustarInventario(new AjustarInventario.AjusteInventarioCommand(
				command.itemSustitutoId(), command.cantidad().negate(),
				"Sustitución en pedido " + command.pedidoId(), null, command.actor()));

		Sustitucion sustitucion = sustitucionRepository.save(new Sustitucion(command.pedidoId(),
				command.itemOriginalId(), command.itemSustitutoId(), command.cantidad(), diferenciaPrecio,
				LocalDateTime.now(), command.observaciones()));

		if (diferenciaPrecio.signum() != 0) {
			registrarCobranza.registrar(new RegistrarCobranza.RegistrarCobranzaCommand(pedido.getClienteId(),
					command.pedidoId(), diferenciaPrecio, FormaPago.OTRO,
					"Sustitución pedido " + command.pedidoId() + " (" + command.itemOriginalId() + "→"
							+ command.itemSustitutoId() + ")"));
		}

		return sustitucion;
	}
}
