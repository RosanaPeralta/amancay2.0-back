package com.amancay.infrastructure.adapter.out.persistence.mapper;

import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;

// Traduce la paginacion del dominio a la de Spring Data y vuelta.
public final class PageMapper {

    public static final Sort NEWEST_FIRST = Sort.by("createdAt").descending();

    private PageMapper() {
    }

    public static Pageable toPageable(PageQuery page, Sort sort) {
        return PageRequest.of(page.page(), page.size(), sort);
    }

    public static <E, T> PageResult<T> toPageResult(Page<E> page, Function<? super E, ? extends T> mapper) {
        return new PageResult<>(page.getContent().stream().<T>map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
