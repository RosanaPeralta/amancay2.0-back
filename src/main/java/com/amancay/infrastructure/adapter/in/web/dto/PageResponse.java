package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.List;
import java.util.function.Function;

import com.amancay.domain.model.PageResult;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> from(PageResult<S> page, Function<? super S, ? extends T> mapper) {
        return new PageResponse<>(page.content().stream().<T>map(mapper).toList(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }
}
