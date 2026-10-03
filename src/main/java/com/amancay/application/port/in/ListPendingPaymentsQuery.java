package com.amancay.application.port.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.Payment;

public interface ListPendingPaymentsQuery {

    // Pagos PENDIENTE de TODOS los usuarios, mas viejos primero: el dueno de la tienda
    // chequeando transferencias contra su cuenta bancaria.
    List<PendingPayment> listPending(UUID requesterId);

    record PendingPayment(Payment payment, String buyerEmail) {
    }
}
