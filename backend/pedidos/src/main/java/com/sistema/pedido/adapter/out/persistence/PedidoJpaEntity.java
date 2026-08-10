package com.sistema.pedido.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.pedido.model.EstadoPedido;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class PedidoJpaEntity extends BaseEntity {

	@Column(nullable = false, unique = true, length = 20)
	private String numero;

	@Column(name = "cliente_id", nullable = false)
	private Long clienteId;

	@Column(name = "vendedor_id", nullable = false)
	private Long vendedorId;

	@Column(name = "pedido_padre_id")
	private Long pedidoPadreId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoPedido estado;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_jornada")
	private LocalDate fechaJornada;

	@Column(length = 255)
	private String observaciones;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "pedido_id", nullable = false)
	private List<PedidoItemJpaEntity> items = new ArrayList<>();

	protected PedidoJpaEntity() {
		// required by JPA
	}

	public PedidoJpaEntity(String numero, Long clienteId, Long vendedorId, Long pedidoPadreId, EstadoPedido estado,
			LocalDateTime fechaCreacion, LocalDate fechaJornada, String observaciones, BigDecimal total) {
		this.numero = numero;
		this.clienteId = clienteId;
		this.vendedorId = vendedorId;
		this.pedidoPadreId = pedidoPadreId;
		this.estado = estado;
		this.fechaCreacion = fechaCreacion;
		this.fechaJornada = fechaJornada;
		this.observaciones = observaciones;
		this.total = total;
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

	public List<PedidoItemJpaEntity> getItems() {
		return items;
	}

	public void setItems(List<PedidoItemJpaEntity> items) {
		this.items = items;
	}
}
