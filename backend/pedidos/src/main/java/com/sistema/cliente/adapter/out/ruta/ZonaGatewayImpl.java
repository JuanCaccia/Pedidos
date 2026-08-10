package com.sistema.cliente.adapter.out.ruta;

import com.sistema.cliente.port.out.ZonaRepository;
import com.sistema.ruta.port.out.ZonaGateway;
import org.springframework.stereotype.Component;

@Component
public class ZonaGatewayImpl implements ZonaGateway {

	private final ZonaRepository zonaRepository;

	public ZonaGatewayImpl(ZonaRepository zonaRepository) {
		this.zonaRepository = zonaRepository;
	}

	@Override
	public boolean existeZona(Long zonaId) {
		return zonaRepository.findById(zonaId).isPresent();
	}
}
