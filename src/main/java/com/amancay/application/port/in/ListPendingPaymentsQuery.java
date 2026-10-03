package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Payment;

public interface ListPendingPaymentsQuery {

    // Pagos PENDIENTE de todos los usuarios, mas viejos primero.
    List<PendingPayment> listPending(UUID requesterId);

    record PendingPayment(Payment payment, String buyerEmail) {
    }
}
