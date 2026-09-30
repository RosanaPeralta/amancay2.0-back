package com.amancay.payment.application.port.out;

import com.amancay.payment.domain.model.Payment;

public interface SavePaymentPort {
    // Devuelve el pago tal como quedo persistido (con id y timestamps asignados).
    Payment save(Payment payment);
}
