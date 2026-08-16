package com.sistema.stock.adapter.out.persistence;

import com.sistema.common.model.BaseEntity;
import com.sistema.stock.model.LoteEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lote")
public class LoteJpaEntity extends BaseEntity {

	@Column(name = "item_id", nullable = false)
	private Long itemId;

	@Column(name = "proveedor_id")
	private Long proveedorId;

	@Column(name = "codigo_lote", nullable = false, length = 100)
	private String codigoLote;

	@Column(name = "fecha_ingreso", nullable = false)
	private LocalDate fechaIngreso;

	@Column(name = "fecha_vencimiento")
	private LocalDate fechaVencimiento;

	@Column(name = "cantidad_ingresada", nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidadIngresada;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private LoteEstado estado = LoteEstado.VIGENTE;

	protected LoteJpaEntity() {
		// required by JPA
	}

	public LoteJpaEntity(Long itemId, String codigoLote, LocalDate fechaIngreso, LocalDate fechaVencimiento, BigDecimal cantidadIngresada) {
		this.itemId = itemId;
		this.codigoLote = codigoLote;
		this.fechaIngreso = fechaIngreso;
		this.fechaVencimiento = fechaVencimiento;
		this.cantidadIngresada = cantidadIngresada;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public Long getProveedorId() {
		return proveedorId;
	}

	public void setProveedorId(Long proveedorId) {
		this.proveedorId = proveedorId;
	}

	public String getCodigoLote() {
		return codigoLote;
	}

	public void setCodigoLote(String codigoLote) {
		this.codigoLote = codigoLote;
	}

	public LocalDate getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(LocalDate fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public void setFechaVencimiento(LocalDate fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	public BigDecimal getCantidadIngresada() {
		return cantidadIngresada;
	}

	public void setCantidadIngresada(BigDecimal cantidadIngresada) {
		this.cantidadIngresada = cantidadIngresada;
	}

	public LoteEstado getEstado() {
		return estado;
	}

	public void setEstado(LoteEstado estado) {
		this.estado = estado;
	}
}
