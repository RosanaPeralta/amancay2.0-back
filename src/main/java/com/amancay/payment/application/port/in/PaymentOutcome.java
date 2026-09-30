package com.amancay.payment.application.port.in;

import com.amancay.payment.domain.model.Payment;

// Resultado de un intento de cobro: el pago persistido y, si la estrategia dio uno,
// el motivo (ej: "Card declined"), que no se guarda en la base.
public record PaymentOutcome(Payment payment, String reason) {
}
