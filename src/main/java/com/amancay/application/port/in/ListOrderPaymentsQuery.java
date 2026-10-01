package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Payment;

public interface ListOrderPaymentsQuery {
    // Mas nuevos primero.
    List<Payment> listByOrder(UUID requesterId, UUID orderId);
}
