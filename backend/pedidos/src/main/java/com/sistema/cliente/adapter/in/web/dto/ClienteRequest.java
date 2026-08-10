package com.sistema.cliente.adapter.in.web.dto;

public record ClienteRequest(String razonSocial, String cuit, String email, String telefono, String domicilio, Long zonaId) {
}
