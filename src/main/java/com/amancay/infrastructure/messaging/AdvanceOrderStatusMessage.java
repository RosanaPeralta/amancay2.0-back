package com.amancay.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.domain.model.OrderStatus;

// Forma en la que viaja AdvanceOrderStatusCommand por RabbitMQ (JSON). Lo publica la
// logistica externa (por ejemplo la consola de logistica); el backend solo lo consume.
public record AdvanceOrderStatusMessage(UUID messageId, UUID orderId, OrderStatus targetStatus, String requestedBy,
        Instant requestedAt) {

    // orderId y targetStatus son obligatorios; el resto se completa si falta.
    public AdvanceOrderStatusCommand toCommand(Instant now) {
        if (orderId == null || targetStatus == null) {
            throw new IllegalArgumentException("orderId and targetStatus are required");
        }
        return new AdvanceOrderStatusCommand(messageId == null ? UUID.randomUUID() : messageId, orderId, targetStatus,
                requestedBy == null || requestedBy.isBlank() ? "unknown" : requestedBy,
                requestedAt == null ? now : requestedAt);
    }
}
