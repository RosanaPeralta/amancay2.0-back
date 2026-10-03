package com.amancay.domain.model;

import java.util.List;
import java.util.function.Function;

// Una pagina de resultados, sin depender de Spring Data (el adaptador traduce su Page).
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
