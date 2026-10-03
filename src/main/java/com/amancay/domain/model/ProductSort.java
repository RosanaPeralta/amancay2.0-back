package com.amancay.domain.model;

import java.util.Locale;

// Ordenes que acepta el listado de productos: sort=name|name_desc|newest.
public enum ProductSort {
    NAME,
    NAME_DESC,
    NEWEST;

    // Case-insensitive; un valor desconocido termina en 400 via IllegalArgumentException.
    public static ProductSort from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("sort must be one of name, name_desc, newest; but was '" + value + "'");
        }
    }
}
