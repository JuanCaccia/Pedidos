package com.sistema.pedido.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Pedido {

	private Long id;
	private String numero;
	private Long clienteId;
	private Long vendedorId;
	private Long pedidoPadreId;
	private EstadoPedido estado = EstadoPedido.PENDIENTE_CONFIRMACION;
	private LocalDateTime fechaCreacion;
	private LocalDateTime updatedAt;
	private LocalDate fechaJornada;
	private String observaciones;
	private BigDecimal total = BigDecimal.ZERO;
	private List<PedidoItem> items = new ArrayList<>();

	public Pedido() {
	}

	public Pedido(Long clienteId, Long vendedorId, LocalDate fechaJornada, String observaciones, List<PedidoItem> items) {
		this.clienteId = clienteId;
		this.vendedorId = vendedorId;
		this.fechaJornada = fechaJornada;
		this.observaciones = observaciones;
		this.items = items;
	}

	public void agregarItem(PedidoItem item) {
		this.items.add(item);
		recalcularTotal();
	}

	public void recalcularTotal() {
		this.total = this.items.stream()
				.map(i -> i.getPrecioUnitario().multiply(i.getCantidadPedida()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Optional<PedidoItem> itemPorItem(Long itemId) {
		return this.items.stream().filter(i -> i.getItemId().equals(itemId)).findFirst();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public Long getVendedorId() {
		return vendedorId;
	}

	public void setVendedorId(Long vendedorId) {
		this.vendedorId = vendedorId;
	}

	public Long getPedidoPadreId() {
		return pedidoPadreId;
	}

	public void setPedidoPadreId(Long pedidoPadreId) {
		this.pedidoPadreId = pedidoPadreId;
	}

	public EstadoPedido getEstado() {
		return estado;
	}

	public void setEstado(EstadoPedido estado) {
		this.estado = estado;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public LocalDate getFechaJornada() {
		return fechaJornada;
	}

	public void setFechaJornada(LocalDate fechaJornada) {
		this.fechaJornada = fechaJornada;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public List<PedidoItem> getItems() {
		return items;
	}

	public void setItems(List<PedidoItem> items) {
		this.items = items;
	}
}
