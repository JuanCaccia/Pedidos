package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Remito;
import com.sistema.cobranza.model.RemitoLinea;
import com.sistema.cobranza.port.in.ConsultarRemito;
import com.sistema.cobranza.port.out.RemitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RemitoService implements ConsultarRemito {

	public record LineaRemito(Long itemId, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	private final RemitoRepository remitoRepository;

	public RemitoService(RemitoRepository remitoRepository) {
		this.remitoRepository = remitoRepository;
	}

	@Transactional
	public Remito generarRemito(Long pedidoId, Long clienteId, List<LineaRemito> lineas) {
		Remito remito = new Remito(pedidoId, clienteId);
		remito.setNumero("REM-" + String.format("%06d", System.nanoTime() % 1000000));
		for (LineaRemito linea : lineas) {
			remito.agregarLinea(new RemitoLinea(linea.itemId(), linea.cantidad(), linea.precioUnitario()));
		}
		return remitoRepository.save(remito);
	}

	@Override
	public Optional<Remito> buscarPorId(Long id) {
		return remitoRepository.findById(id);
	}

	@Override
	public List<Remito> listar(Long pedidoId, Long clienteId) {
		if (pedidoId != null) {
			return remitoRepository.findByPedidoId(pedidoId);
		}
		if (clienteId != null) {
			return remitoRepository.findByClienteId(clienteId);
		}
		return remitoRepository.findAll();
	}
}
