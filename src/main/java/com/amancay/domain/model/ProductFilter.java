package com.amancay.domain.model;

import java.util.UUID;

// Filtros opcionales del listado de productos. Cada filtro en null se omite.
public record ProductFilter(String name, UUID categoryId, Boolean active) {

    public ProductFilter {
        name = name == null || name.isBlank() ? null : name.trim();
    }
}
