package com.sistema.reporte.service;

import com.sistema.pedido.model.EstadoPedido;
import com.sistema.pedido.model.Pedido;
import com.sistema.pedido.port.in.ConsultarPedido;
import com.sistema.reporte.port.in.ConsultarReportes;
import com.sistema.ruta.port.in.ConsultarRuta;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.usuario.port.in.ConsultarUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReporteService implements ConsultarReportes {

	private final ConsultarStock consultarStock;
	private final ConsultarPedido consultarPedido;
	private final ConsultarUsuario consultarUsuario;
	private final ConsultarRuta consultarRuta;

	public ReporteService(ConsultarStock consultarStock, ConsultarPedido consultarPedido,
			ConsultarUsuario consultarUsuario, ConsultarRuta consultarRuta) {
		this.consultarStock = consultarStock;
		this.consultarPedido = consultarPedido;
		this.consultarUsuario = consultarUsuario;
		this.consultarRuta = consultarRuta;
	}

	@Override
	public List<ItemStockReporte> stockGeneral() {
		return consultarStock.listarItems().stream()
				.map(item -> new ItemStockReporte(item.getId(), item.getSku(), item.getNombre(),
						consultarStock.obtenerDisponible(item.getId()),
						consultarStock.obtenerReservasActivas(item.getId())))
				.toList();
	}

	@Override
	public List<VentaVendedorReporte> ventasPorVendedor(Long vendedorId, LocalDate desde, LocalDate hasta) {
		Map<Long, VentasAcumuladas> porVendedor = new LinkedHashMap<>();
		for (Pedido pedido : consultarPedido.listarTodos()) {
			if (pedido.getEstado() != EstadoPedido.ENTREGADO
					&& pedido.getEstado() != EstadoPedido.ENTREGADO_PARCIAL) {
				continue;
			}
			if (vendedorId != null && !vendedorId.equals(pedido.getVendedorId())) {
				continue;
			}
			if (desde != null && pedido.getFechaCreacion().toLocalDate().isBefore(desde)) {
				continue;
			}
			if (hasta != null && pedido.getFechaCreacion().toLocalDate().isAfter(hasta)) {
				continue;
			}
			VentasAcumuladas acum = porVendedor.computeIfAbsent(pedido.getVendedorId(), k -> new VentasAcumuladas());
			acum.pedidos++;
			for (var item : pedido.getItems()) {
				acum.unidades = acum.unidades.add(item.getCantidadEntregada());
				acum.monto = acum.monto.add(item.getCantidadEntregada().multiply(item.getPrecioUnitario()));
			}
		}
		List<VentaVendedorReporte> resultado = new ArrayList<>();
		porVendedor.forEach((vendedor, acum) -> {
			String nombre = consultarUsuario.buscarPorId(vendedor)
					.map(u -> u.getNombre())
					.orElse("Usuario " + vendedor);
			resultado.add(new VentaVendedorReporte(vendedor, nombre, acum.pedidos, acum.unidades, acum.monto));
		});
		return resultado;
	}

	@Override
	public List<RutaReporte> rutasPorFecha(LocalDate fecha) {
		List<com.sistema.ruta.model.Ruta> rutas = fecha == null
				? consultarRuta.listarTodos()
				: consultarRuta.listarPorFecha(fecha);
		return rutas.stream()
				.map(r -> new RutaReporte(r.getId(), r.getZonaId(), r.getRepartidorId(), r.getFechaJornada(),
						r.getEstado().name(), r.getPedidoIds().size()))
				.toList();
	}

	private static class VentasAcumuladas {
		long pedidos;
		BigDecimal unidades = BigDecimal.ZERO;
		BigDecimal monto = BigDecimal.ZERO;
	}
}
