package com.sistema.ruta.port.in;

import com.sistema.ruta.model.Ruta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GestionarRuta {

	record CrearRutaCommand(Long zonaId, Long repartidorId, LocalDate fechaJornada, List<Long> pedidoIds,
			BigDecimal capacidadBultos) {
	}

	Ruta crearRuta(CrearRutaCommand command);

	Ruta asignarPedidos(Long rutaId, List<Long> pedidoIds);

	Ruta iniciarJornada(Long rutaId);

	Ruta cerrarJornada(Long rutaId);
}
