package com.sistema.pedido.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservaTtlJob {

	private static final Logger log = LoggerFactory.getLogger(ReservaTtlJob.class);

	private final PedidoService pedidoService;

	public ReservaTtlJob(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@Scheduled(fixedDelayString = "${app.pedido.reserva-ttl-job-ms:3600000}")
	public void ejecutar() {
		try {
			int expirados = pedidoService.expirarReservasInactivas();
			if (expirados > 0) {
				log.info("TTL de reservas: {} pedidos auto-cancelados por inactividad", expirados);
			}
		} catch (RuntimeException e) {
			log.error("Error en el job TTL de reservas", e);
		}
	}
}
