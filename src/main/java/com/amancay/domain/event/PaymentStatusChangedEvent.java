package com.amancay.domain.event;

import java.util.UUID;

import com.amancay.domain.model.PaymentStatus;

// Se publica despues de persistir el resultado de un intento de pago o la decision del
// admin sobre una transferencia (ver OrderStatusChangedEvent, mismo mecanismo).
public record PaymentStatusChangedEvent(UUID paymentId, PaymentStatus previousStatus, PaymentStatus newStatus) {
}
