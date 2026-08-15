package com.sistema.stock.adapter.in.web;

import com.sistema.stock.adapter.in.web.dto.ActualizarItemRequest;
import com.sistema.stock.adapter.in.web.dto.ItemRequest;
import com.sistema.stock.adapter.in.web.dto.ItemResponse;
import com.sistema.stock.model.Item;
import com.sistema.stock.port.in.ConsultarStock;
import com.sistema.stock.port.in.GestionarItem;
import com.sistema.common.exception.NotFoundException;
import com.sistema.common.model.PageMapper;
import com.sistema.common.model.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public ResponseEntity<ItemResponse> crear(@Valid @RequestBody ItemRequest request) {
		Item item = gestionarItem.crearItem(new GestionarItem.CrearItemCommand(
				request.sku(), request.nombre(), request.unidadMedida(), request.stockMinimo(), request.precioLista(), request.categoria()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
	}

	@GetMapping
	public PageResponse<ItemResponse> listar(@RequestParam(required = false) String q,
			@RequestParam(required = false) String categoria,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		PageResponse<Item> pagina = consultarStock.listarItemsPaginado(q, categoria, page, size);
		return PageMapper.of(pagina.content(), pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages(), ItemResponse::from);
	}

	@GetMapping("/categorias")
	public List<String> categorias() {
		return consultarStock.listarCategorias();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ItemResponse> buscarPorId(@PathVariable Long id) {
		Item item = consultarStock.buscarItemPorId(id)
				.orElseThrow(() -> new NotFoundException("Item no encontrado: " + id));
		return ResponseEntity.ok(ItemResponse.from(item));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ItemResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarItemRequest request) {
		Item item = gestionarItem.actualizarItem(new GestionarItem.ActualizarItemCommand(
				id, request.nombre(), request.unidadMedida(), request.stockMinimo(), request.precioLista(), request.categoria()));
		return ResponseEntity.ok(ItemResponse.from(item));
	}

	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		gestionarItem.desactivarItem(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/reactivar")
	public ResponseEntity<Void> reactivar(@PathVariable Long id) {
		gestionarItem.reactivarItem(id);
		return ResponseEntity.noContent().build();
	}
}
