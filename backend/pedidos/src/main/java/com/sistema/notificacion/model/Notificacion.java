package com.sistema.notificacion.model;

import java.time.LocalDateTime;

public class Notificacion {

	private Long id;
	private String tipo;
	private String mensaje;
	private Long paraUsuarioId;
	private Long pedidoId;
	private boolean leida;
	private LocalDateTime fecha;

	public Notificacion() {
	}

	public Notificacion(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId) {
		this.tipo = tipo;
		this.mensaje = mensaje;
		this.paraUsuarioId = paraUsuarioId;
		this.pedidoId = pedidoId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public Long getParaUsuarioId() {
		return paraUsuarioId;
	}

	public void setParaUsuarioId(Long paraUsuarioId) {
		this.paraUsuarioId = paraUsuarioId;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public boolean isLeida() {
		return leida;
	}

	public void setLeida(boolean leida) {
		this.leida = leida;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
