package com.amancay.dto;

import java.util.Map;

public record RatingSummaryDto(
        Double average,
        long total,
        Map<Integer, Long> distribution) {
}
