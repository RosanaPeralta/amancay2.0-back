package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.ReturnReason;
import com.amancay.domain.model.ReturnRequest;
import com.amancay.domain.model.ReturnRequestStatus;

public record ReturnRequestResponse(
        UUID id,
        UUID orderId,
        ReturnReason reason,
        String comment,
        ReturnRequestStatus status,
        Instant requestedAt,
        Instant resolvedAt,
        UUID resolvedBy,
        String resolutionNote) {

    public static ReturnRequestResponse from(ReturnRequest request) {
        return new ReturnRequestResponse(
                request.getId(),
                request.getOrderId(),
                request.getReason(),
                request.getComment(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getResolvedAt(),
                request.getResolvedBy(),
                request.getResolutionNote());
    }
}