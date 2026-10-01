package com.amancay.infrastructure.adapter.in.web.dto;

import com.amancay.domain.model.ReviewStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
        @NotNull ReviewStatus status) {
}
