package com.sistema.stock.port.in;

import com.sistema.stock.model.Item;

import java.math.BigDecimal;

public interface GestionarItem {

	record CrearItemCommand(String sku, String nombre, String unidadMedida, BigDecimal stockMinimo, BigDecimal precioLista, String categoria) {
	}

	record ActualizarItemCommand(Long itemId, String nombre, String unidadMedida, BigDecimal stockMinimo, BigDecimal precioLista, String categoria) {
	}

	Item crearItem(CrearItemCommand command);

	Item actualizarItem(ActualizarItemCommand command);

	void desactivarItem(Long itemId);

	void reactivarItem(Long itemId);
}
