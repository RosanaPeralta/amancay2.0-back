package com.amancay.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Promedio (redondeado a un decimal, null sin resenas), total y cuantas hay de cada puntaje,
// con todos los puntajes presentes aunque tengan 0.
public record RatingSummary(Double average, long total, Map<Integer, Long> distribution) {

    public static RatingSummary from(List<RatingCount> rows) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = Review.MIN_RATING; rating <= Review.MAX_RATING; rating++) {
            distribution.put(rating, 0L);
        }
        long total = 0L;
        long weightedSum = 0L;
        for (RatingCount row : rows) {
            distribution.put(row.rating(), row.count());
            total += row.count();
            weightedSum += (long) row.rating() * row.count();
        }
        Double average = total == 0 ? null : Math.round((double) weightedSum / total * 10.0) / 10.0;
        return new RatingSummary(average, total, Collections.unmodifiableMap(distribution));
    }
}
