package com.sistema.compra.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.compra.model.EstadoOrdenCompra;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden_compra")
public class OrdenCompraJpaEntity extends BaseEntity {

	@Column(nullable = false, unique = true, length = 20)
	private String numero;

	@Column(name = "proveedor_id", nullable = false)
	private Long proveedorId;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoOrdenCompra estado;

	@Column(length = 255)
	private String observaciones;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "orden_compra_id", nullable = false)
	private List<OrdenCompraLineaJpaEntity> lineas = new ArrayList<>();

	protected OrdenCompraJpaEntity() {
		// required by JPA
	}

	public OrdenCompraJpaEntity(String numero, Long proveedorId, LocalDateTime fecha, EstadoOrdenCompra estado,
			String observaciones) {
		this.numero = numero;
		this.proveedorId = proveedorId;
		this.fecha = fecha;
		this.estado = estado;
		this.observaciones = observaciones;
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

	public List<OrdenCompraLineaJpaEntity> getLineas() {
		return lineas;
	}

	public void setLineas(List<OrdenCompraLineaJpaEntity> lineas) {
		this.lineas = lineas;
	}
}
