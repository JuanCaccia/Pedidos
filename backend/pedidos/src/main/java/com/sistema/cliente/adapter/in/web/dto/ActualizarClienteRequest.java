package com.sistema.cliente.adapter.in.web.dto;

public record ActualizarClienteRequest(String razonSocial, String email, String telefono, String domicilio, Long zonaId) {
}
