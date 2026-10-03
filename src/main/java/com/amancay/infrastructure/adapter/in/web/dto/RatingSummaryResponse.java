package com.amancay.infrastructure.adapter.in.web.dto;

import java.util.Map;

import com.amancay.domain.model.RatingSummary;

public record RatingSummaryResponse(
        Double average,
        long total,
        Map<Integer, Long> distribution) {

    public static RatingSummaryResponse from(RatingSummary summary) {
        return new RatingSummaryResponse(summary.average(), summary.total(), summary.distribution());
    }
}
