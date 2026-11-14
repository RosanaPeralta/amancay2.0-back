
package com.amancay.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ReturnRequest {

    private final UUID id;
    private final UUID orderId;
    private final ReturnReason reason;
    private final String comment;
    private ReturnRequestStatus status;
    private final Instant requestedAt;
    private Instant resolvedAt;
    private UUID resolvedBy;
    private String resolutionNote;

    // Reconstruye una solicitud existente desde persistencia.
    public ReturnRequest(
            UUID id,
            UUID orderId,
            ReturnReason reason,
            String comment,
            ReturnRequestStatus status,
            Instant requestedAt,
            Instant resolvedAt,
            UUID resolvedBy,
            String resolutionNote) {

        this.id = id;
        this.orderId = Objects.requireNonNull(orderId, "orderId is required");
        this.reason = Objects.requireNonNull(reason, "reason is required");
        this.comment = normalizeOptionalText(comment);
        this.status = Objects.requireNonNull(status, "status is required");
        this.requestedAt = requestedAt;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.resolutionNote = normalizeOptionalText(resolutionNote);
    }


    public static ReturnRequest create(
            UUID orderId,
            ReturnReason reason,
            String comment) {
        return create(orderId, reason, comment, Instant.now());
    }

    public static ReturnRequest create(
            UUID orderId,
            ReturnReason reason,
            String comment,
            Instant requestedAt) {

        return new ReturnRequest(
                null,
                orderId,
                reason,
                comment,
                ReturnRequestStatus.PENDIENTE,
                Objects.requireNonNull(requestedAt, "requestedAt is required"),
                null,
                null,
                null);
    }

    public void approve(UUID adminId) {
        approve(adminId, Instant.now());
    }

    public void approve(UUID adminId, Instant resolvedAt) {
        ensurePending();
        Objects.requireNonNull(adminId, "adminId is required");
        Objects.requireNonNull(resolvedAt, "resolvedAt is required");

        this.status = ReturnRequestStatus.APROBADA;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = adminId;
        this.resolutionNote = null;
    }

    public void reject(UUID adminId, String resolutionNote) {
        reject(adminId, resolutionNote, Instant.now());
    }

    public void reject(UUID adminId, String resolutionNote, Instant resolvedAt) {
        ensurePending();
        Objects.requireNonNull(adminId, "adminId is required");
        Objects.requireNonNull(resolvedAt, "resolvedAt is required");

        String note = normalizeOptionalText(resolutionNote);

        if (note == null) {
            throw new IllegalArgumentException(
                    "A resolution note is required when rejecting a return request");
        }

        this.status = ReturnRequestStatus.RECHAZADA;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = adminId;
        this.resolutionNote = note;
    }

    private void ensurePending() {
        if (!status.isPending()) {
            throw new IllegalStateException(
                    "Only pending return requests can be resolved");
        }
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public boolean isPending() {
        return status.isPending();
    }

    public boolean isResolved() {
        return status.isResolved();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public ReturnReason getReason() {
        return reason;
    }

    public String getComment() {
        return comment;
    }

    public ReturnRequestStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
}