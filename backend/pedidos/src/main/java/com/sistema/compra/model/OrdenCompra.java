package com.sistema.compra.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrdenCompra {

	private Long id;
	private String numero;
	private Long proveedorId;
	private LocalDateTime fecha;
	private EstadoOrdenCompra estado = EstadoOrdenCompra.PENDIENTE;
	private String observaciones;
	private List<OrdenCompraLinea> lineas = new ArrayList<>();

	public OrdenCompra() {
	}

	public OrdenCompra(Long proveedorId, String observaciones) {
		this.proveedorId = proveedorId;
		this.observaciones = observaciones;
		this.fecha = LocalDateTime.now();
	}

	public void agregarLinea(OrdenCompraLinea linea) {
		this.lineas.add(linea);
	}

	public Optional<OrdenCompraLinea> lineaPorId(Long lineaId) {
		return this.lineas.stream().filter(l -> l.getId().equals(lineaId)).findFirst();
	}

	public Optional<OrdenCompraLinea> lineaPorItemId(Long itemId) {
		return this.lineas.stream().filter(l -> l.getItemId().equals(itemId)).findFirst();
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

	public Long getProveedorId() {
		return proveedorId;
	}

	public void setProveedorId(Long proveedorId) {
		this.proveedorId = proveedorId;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public EstadoOrdenCompra getEstado() {
		return estado;
	}

	public void setEstado(EstadoOrdenCompra estado) {
		this.estado = estado;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}

	public List<OrdenCompraLinea> getLineas() {
		return lineas;
	}

	public void setLineas(List<OrdenCompraLinea> lineas) {
		this.lineas = lineas;
	}
}
