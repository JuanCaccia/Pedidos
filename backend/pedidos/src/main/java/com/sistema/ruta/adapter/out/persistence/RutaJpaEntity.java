package com.sistema.ruta.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.ruta.model.EstadoRuta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ruta")
public class RutaJpaEntity extends BaseEntity {

	@Column(name = "zona_id", nullable = false)
	private Long zonaId;

	@Column(name = "repartidor_id", nullable = false)
	private Long repartidorId;

	@Column(name = "fecha_jornada", nullable = false)
	private LocalDate fechaJornada;

	@Column(name = "capacidad_bultos", nullable = false, precision = 12, scale = 3)
	private BigDecimal capacidadBultos = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoRuta estado;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "ruta_id", nullable = false)
	private List<RutaPedidoJpaEntity> pedidos = new ArrayList<>();

	protected RutaJpaEntity() {
		// required by JPA
	}

	public RutaJpaEntity(Long zonaId, Long repartidorId, LocalDate fechaJornada, EstadoRuta estado) {
		this.zonaId = zonaId;
		this.repartidorId = repartidorId;
		this.fechaJornada = fechaJornada;
		this.estado = estado;
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

	public List<RutaPedidoJpaEntity> getPedidos() {
		return pedidos;
	}

	public void setPedidos(List<RutaPedidoJpaEntity> pedidos) {
		this.pedidos = pedidos;
	}
}
