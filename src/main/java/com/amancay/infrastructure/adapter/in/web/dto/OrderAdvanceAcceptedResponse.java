package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.application.port.in.AdvanceOrderStatusCommand;
import com.amancay.domain.model.OrderStatus;

// Respuesta 202: el cambio quedo encolado, todavia no se aplico.
public record OrderAdvanceAcceptedResponse(UUID commandId, UUID orderId, OrderStatus targetStatus,
        String requestedBy, Instant requestedAt) {

    public static OrderAdvanceAcceptedResponse from(AdvanceOrderStatusCommand command) {
        return new OrderAdvanceAcceptedResponse(command.commandId(), command.orderId(), command.targetStatus(),
                command.requestedBy(), command.requestedAt());
    }
}
