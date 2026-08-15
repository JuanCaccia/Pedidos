package com.sistema.cobranza.service;

import com.sistema.cobranza.model.Remito;
import com.sistema.cobranza.port.out.RemitoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RemitoServiceTest {

	private RemitoService remitoService;
	private FakeRemitoRepository remitoRepository;

	@BeforeEach
	void setUp() {
		remitoRepository = new FakeRemitoRepository();
		remitoService = new RemitoService(remitoRepository);
	}

	@Test
	void generarRemitoCalculaMontoYLineas() {
		Remito remito = remitoService.generarRemito(10L, 1L, List.of(
				new RemitoService.LineaRemito(100L, new BigDecimal("2.000"), new BigDecimal("5.00")),
				new RemitoService.LineaRemito(200L, new BigDecimal("1.000"), new BigDecimal("3.50"))));

		assertNotNull(remito.getNumero());
		assertEquals(2, remito.getLineas().size());
		assertEquals(0, new BigDecimal("13.50").compareTo(remito.getMontoTotal()));
	}

	private static class FakeRemitoRepository implements RemitoRepository {

		private final Map<Long, Remito> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Remito save(Remito remito) {
			if (remito.getId() == null) {
				remito.setId(secuencia.getAndIncrement());
			}
			datos.put(remito.getId(), remito);
			return remito;
		}

		@Override
		public Optional<Remito> findById(Long id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public List<Remito> findByPedidoId(Long pedidoId) {
			return datos.values().stream().filter(r -> r.getPedidoId().equals(pedidoId)).toList();
		}

		@Override
		public List<Remito> findByClienteId(Long clienteId) {
			return datos.values().stream().filter(r -> r.getClienteId().equals(clienteId)).toList();
		}

		@Override
		public List<Remito> findAll() {
			return new ArrayList<>(datos.values());
		}
	}
}
