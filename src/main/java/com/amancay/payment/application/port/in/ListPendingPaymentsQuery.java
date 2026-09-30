package com.amancay.payment.application.port.in;

import java.util.List;

import com.amancay.payment.domain.model.Payment;

public interface ListPendingPaymentsQuery {

    // Pagos PENDIENTE de TODOS los usuarios, mas viejos primero: el dueno de la tienda
    // chequeando transferencias contra su cuenta bancaria.
    List<PendingPayment> listPending();

    record PendingPayment(Payment payment, String buyerEmail) {
    }
}
