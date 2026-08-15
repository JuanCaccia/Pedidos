package com.sistema.common.model;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class PageMapper {

	private PageMapper() {
	}

	public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> mapper) {
		List<R> content = page.getContent().stream().map(mapper).toList();
		return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	public static <T, R> PageResponse<R> of(List<T> content, int page, int size, long totalElements, int totalPages, Function<T, R> mapper) {
		return new PageResponse<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
	}
}
