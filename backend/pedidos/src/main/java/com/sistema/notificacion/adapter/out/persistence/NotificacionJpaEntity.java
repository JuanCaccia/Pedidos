package com.sistema.notificacion.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
public class NotificacionJpaEntity extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String tipo;

	@Column(nullable = false, length = 500)
	private String mensaje;

	@Column(name = "para_usuario_id", nullable = false)
	private Long paraUsuarioId;

	@Column(name = "pedido_id")
	private Long pedidoId;

	@Column(nullable = false)
	private boolean leida = false;

	@Column(nullable = false)
	private LocalDateTime fecha;

	protected NotificacionJpaEntity() {
		// required by JPA
	}

	public NotificacionJpaEntity(String tipo, String mensaje, Long paraUsuarioId, Long pedidoId, boolean leida,
			LocalDateTime fecha) {
		this.tipo = tipo;
		this.mensaje = mensaje;
		this.paraUsuarioId = paraUsuarioId;
		this.pedidoId = pedidoId;
		this.leida = leida;
		this.fecha = fecha;
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
