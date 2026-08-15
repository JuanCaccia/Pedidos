package com.sistema.notificacion.service;

import com.sistema.common.exception.BusinessException;
import com.sistema.notificacion.model.Notificacion;
import com.sistema.notificacion.port.in.GestionarNotificacion;
import com.sistema.notificacion.port.out.NotificacionRepository;
import com.sistema.notificacion.port.out.UsuarioGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionServiceTest {

	private NotificacionService notificacionService;
	private FakeNotificacionRepository repository;

	@BeforeEach
	void setUp() {
		repository = new FakeNotificacionRepository();
		notificacionService = new NotificacionService(repository, id -> id.equals(1L));
	}

	@Test
	void notificarValidaYGuarda() {
		Notificacion guardada = notificacionService.notificar(
				new GestionarNotificacion.NotificarCommand("FALTANTE_PRODUCTO", "Diferencia detectada", 1L, 7L));

		assertNotNull(guardada.getId());
		assertEquals("FALTANTE_PRODUCTO", guardada.getTipo());
		assertEquals(7L, guardada.getPedidoId());
		assertNotNull(guardada.getFecha());
		assertFalse(guardada.isLeida());
	}

	@Test
	void notificarAUsuarioInexistenteLanza() {
		assertThrows(BusinessException.class, () -> notificacionService.notificar(
				new GestionarNotificacion.NotificarCommand("FALTANTE_PRODUCTO", "msg", 999L, 7L)));
	}

	@Test
	void marcarLeidaSoloParaElDuenio() {
		Notificacion delActor = notificacionService.notificar(
				new GestionarNotificacion.NotificarCommand("T1", "msg", 1L, 7L));
		Notificacion deOtro = repository.guardarDirecto("T1", "msg", 2L, 7L);

		notificacionService.marcarLeida(delActor.getId(), 1L);

		assertTrue(repository.findById(delActor.getId()).isLeida());
		assertFalse(repository.findById(deOtro.getId()).isLeida());
	}

	@Test
	void contarNoLeidasSoloSumaNoLeidas() {
		repository.guardarDirecto("T1", "msg", 1L, 7L);
		repository.guardarDirecto("T2", "msg", 1L, 7L);
		Notificacion leida = repository.guardarDirecto("T3", "msg", 1L, 7L);
		repository.findById(leida.getId()).setLeida(true);

		assertEquals(2L, notificacionService.contarNoLeidas(1L));
	}

	@Test
	void listarFiltraSoloNoLeidas() {
		repository.guardarDirecto("T1", "msg", 1L, 7L);
		Notificacion leida = repository.guardarDirecto("T2", "msg", 1L, 7L);
		repository.findById(leida.getId()).setLeida(true);

		assertEquals(1, notificacionService.listar(1L, true).size());
		assertEquals(2, notificacionService.listar(1L, false).size());
	}

	private static class FakeNotificacionRepository implements NotificacionRepository {

		private final Map<Long, Notificacion> datos = new HashMap<>();
		private final AtomicLong secuencia = new AtomicLong(1);

		@Override
		public Notificacion save(Notificacion n) {
			if (n.getId() == null) {
				n.setId(secuencia.getAndIncrement());
			}
			datos.put(n.getId(), n);
			return n;
		}

		@Override
		public List<Notificacion> findByParaUsuarioId(Long paraUsuarioId) {
			return datos.values().stream().filter(n -> n.getParaUsuarioId().equals(paraUsuarioId)).toList();
		}

		Notificacion guardarDirecto(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId) {
			Notificacion n = new Notificacion(tipo, mensaje, paraUsuarioId, pedidoId);
			n.setFecha(LocalDateTime.now());
			return save(n);
		}

		Notificacion findById(Long id) {
			return datos.get(id);
		}
	}
}
