package com.sistema.stock.port.in;

import com.sistema.stock.model.Item;

public interface GestionarItem {

	record CrearItemCommand(String sku, String nombre, String unidadMedida) {
	}

	Item crearItem(CrearItemCommand command);

	void desactivarItem(Long itemId);
}
