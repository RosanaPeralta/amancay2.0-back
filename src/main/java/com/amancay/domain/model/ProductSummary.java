package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

// Lo minimo de un producto para listados (catalogo, favoritos), sin variantes ni imagenes.
public record ProductSummary(UUID id, String name, String slug, boolean active, Instant createdAt,
        Instant updatedAt) {
}
