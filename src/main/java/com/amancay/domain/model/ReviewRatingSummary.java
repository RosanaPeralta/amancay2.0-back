package com.amancay.domain.model;

import java.util.Map;

public record ReviewRatingSummary(Double average, long total, Map<Integer, Long> distribution) {
    public ReviewRatingSummary {
        distribution = Map.copyOf(distribution);
    }
}