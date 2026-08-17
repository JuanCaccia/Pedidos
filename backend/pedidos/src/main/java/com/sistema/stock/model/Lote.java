package com.sistema.stock.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Lote {

	private Long id;
	private Long itemId;
	private Long proveedorId;
	private String codigoLote;
	private LocalDate fechaIngreso;
	private LocalDate fechaVencimiento;
	private BigDecimal cantidadIngresada;
	private BigDecimal precioUnitario;
	private LoteEstado estado = LoteEstado.VIGENTE;

	public Lote() {
	}

	public Lote(Long itemId, String codigoLote, LocalDate fechaIngreso, LocalDate fechaVencimiento, BigDecimal cantidadIngresada) {
		this.itemId = itemId;
		this.codigoLote = codigoLote;
		this.fechaIngreso = fechaIngreso;
		this.fechaVencimiento = fechaVencimiento;
		this.cantidadIngresada = cantidadIngresada;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public LoteEstado getEstado() {
		return estado;
	}

	public void setEstado(LoteEstado estado) {
		this.estado = estado;
	}
}
