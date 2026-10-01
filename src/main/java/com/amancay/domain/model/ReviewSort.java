package com.amancay.domain.model;

import java.util.Locale;

// Ordenes que acepta el listado publico de resenas: sort=recent|best|worst.
public enum ReviewSort {
    RECENT,
    BEST,
    WORST;

    // Case-insensitive; un valor desconocido termina en 400 via IllegalArgumentException.
    public static ReviewSort from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("sort must be one of recent, best, worst; but was '" + value + "'");
        }
    }
}
