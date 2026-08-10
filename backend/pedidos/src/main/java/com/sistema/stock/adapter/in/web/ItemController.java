package com.sistema.stock.adapter.in.web;

import com.sistema.stock.adapter.in.web.dto.ItemRequest;
import com.sistema.stock.adapter.in.web.dto.ItemResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.stock.port.in.GestionarItem;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/items")
@Tag(name = "Items")
public class ItemController {

	private final GestionarItem gestionarItem;
	private final ConsultarStock consultarStock;

	public ItemController(GestionarItem gestionarItem, ConsultarStock consultarStock) {
		this.gestionarItem = gestionarItem;
		this.consultarStock = consultarStock;
	}

	@PostMapping
	public ResponseEntity<ItemResponse> crear(@RequestBody ItemRequest request) {
		Item item = gestionarItem.crearItem(new GestionarItem.CrearItemCommand(
				request.sku(), request.nombre(), request.unidadMedida()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
	}

	@GetMapping
	public List<ItemResponse> listar() {
		return consultarStock.listarItems().stream().map(ItemResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ItemResponse> buscarPorId(@PathVariable Long id) {
		return consultarStock.buscarItemPorId(id)
				.map(ItemResponse::from)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarItem.desactivarItem(id);
		return ResponseEntity.noContent().build();
	}
}
