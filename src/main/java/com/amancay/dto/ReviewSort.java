package com.amancay.dto;

import org.springframework.data.domain.Sort;

/** Órdenes que acepta el listado público de reseñas (REV-05): {@code sort=recent|best|worst}. */
public enum ReviewSort {
    RECENT(Sort.by("createdAt").descending()),
    BEST(Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"))),
    WORST(Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt")));

    private final Sort sort;

    ReviewSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    /** Case-insensitive; un valor desconocido termina en 400 vía {@code IllegalArgumentException}. */
    public static ReviewSort from(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("sort must be one of recent, best, worst; but was '" + value + "'");
        }
    }
}
