package com.amancay.dto;

/**
 * Fila tipada de {@code ReviewRepository.countPublishedGroupedByRating}: cuántas reseñas
 * publicadas tiene un producto con cada puntaje.
 */
public record RatingCount(int rating, long count) {
}
