package com.sistema.ruta.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Ruta {

	private Long id;
	private Long zonaId;
	private Long repartidorId;
	private LocalDate fechaJornada;
	private BigDecimal capacidadBultos = BigDecimal.ZERO;
	private EstadoRuta estado = EstadoRuta.PLANIFICADA;
	private List<RutaPedido> pedidos = new ArrayList<>();

	public Ruta() {
	}

	public Ruta(Long zonaId, Long repartidorId, LocalDate fechaJornada) {
		this.zonaId = zonaId;
		this.repartidorId = repartidorId;
		this.fechaJornada = fechaJornada;
	}

	public void asignarPedidos(List<Long> ids) {
		for (Long pedidoId : ids) {
			this.pedidos.add(new RutaPedido(pedidoId));
		}
	}

	public List<Long> getPedidoIds() {
		return this.pedidos.stream().map(RutaPedido::getPedidoId).toList();
	}

	public void iniciarJornada() {
		this.estado = EstadoRuta.EN_CURSO;
	}

	public void cerrarJornada() {
		this.estado = EstadoRuta.FINALIZADA;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getZonaId() {
		return zonaId;
	}

	public void setZonaId(Long zonaId) {
		this.zonaId = zonaId;
	}

	public Long getRepartidorId() {
		return repartidorId;
	}

	public void setRepartidorId(Long repartidorId) {
		this.repartidorId = repartidorId;
	}

	public LocalDate getFechaJornada() {
		return fechaJornada;
	}

	public void setFechaJornada(LocalDate fechaJornada) {
		this.fechaJornada = fechaJornada;
	}

	public BigDecimal getCapacidadBultos() {
		return capacidadBultos;
	}

	public void setCapacidadBultos(BigDecimal capacidadBultos) {
		this.capacidadBultos = capacidadBultos;
	}

	public EstadoRuta getEstado() {
		return estado;
	}

	public void setEstado(EstadoRuta estado) {
		this.estado = estado;
	}

	public List<RutaPedido> getPedidos() {
		return pedidos;
	}

	public void setPedidos(List<RutaPedido> pedidos) {
		this.pedidos = pedidos;
	}
}
