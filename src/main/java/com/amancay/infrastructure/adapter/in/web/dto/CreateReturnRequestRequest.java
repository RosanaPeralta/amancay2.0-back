package com.amancay.infrastructure.adapter.in.web.dto;

import com.amancay.domain.model.ReturnReason;

import jakarta.validation.constraints.NotNull;

public record CreateReturnRequestRequest(
        @NotNull ReturnReason reason,
        String comment) {
}