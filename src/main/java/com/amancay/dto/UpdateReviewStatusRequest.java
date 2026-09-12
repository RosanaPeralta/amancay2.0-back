package com.amancay.dto;

import com.amancay.entity.ReviewStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateReviewStatusRequest(
        @NotNull ReviewStatus status) {
}
