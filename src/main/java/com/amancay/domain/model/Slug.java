package com.amancay.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

// Version amigable para la url de un producto, derivada de su nombre.
public final class Slug {

    private Slug() {
    }

    public static String from(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? UUID.randomUUID().toString() : slug;
    }

    public static String withSuffix(String base, int suffix) {
        return base + "-" + suffix;
    }
}
