package com.sistema.compra.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetItemsProveedorRequest(@NotNull List<Long> itemIds) {
}
