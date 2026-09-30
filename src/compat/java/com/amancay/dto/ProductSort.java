package com.amancay.dto;

import org.springframework.data.domain.Sort;

/** Órdenes que acepta el listado de productos: {@code sort=name|name_desc|newest}. */
public enum ProductSort {
    NAME(Sort.by("name").ascending()),
    NAME_DESC(Sort.by("name").descending()),
    NEWEST(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name")));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    /** Case-insensitive; un valor desconocido termina en 400 vía {@code IllegalArgumentException}. */
    public static ProductSort from(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("sort must be one of name, name_desc, newest; but was '" + value + "'");
        }
    }
}
