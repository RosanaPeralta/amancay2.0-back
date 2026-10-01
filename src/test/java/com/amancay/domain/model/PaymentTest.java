package com.amancay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void attemptTakesTheStatusReturnedByTheProcessor() {
        Payment payment = Payment.attempt(UUID.randomUUID(), BigDecimal.TEN, PaymentMethod.TRANSFERENCIA,
                PaymentResult.pending("Awaiting manual confirmation"));

        assertThat(payment.getId()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDIENTE);
        assertThat(payment.isApproved()).isFalse();
    }

    @Test
    void confirmMovesAPendingPaymentToTheDecision() {
        Payment payment = payment(PaymentMethod.TRANSFERENCIA, PaymentStatus.PENDIENTE);

        payment.confirm(PaymentStatus.APROBADO);

        assertThat(payment.isApproved()).isTrue();
    }

    @Test
    void confirmRejectsPendienteAsADecision() {
        Payment payment = payment(PaymentMethod.TRANSFERENCIA, PaymentStatus.PENDIENTE);

        assertThatThrownBy(() -> payment.confirm(PaymentStatus.PENDIENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmRejectsAnAlreadyDecidedPayment() {
        Payment payment = payment(PaymentMethod.TRANSFERENCIA, PaymentStatus.RECHAZADO);

        assertThatThrownBy(() -> payment.confirm(PaymentStatus.APROBADO)).isInstanceOf(IllegalStateException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void onlyRejectedPaymentsCanBeRetried() {
        payment(PaymentMethod.TARJETA_CREDITO, PaymentStatus.RECHAZADO).ensureRetryable();

        assertThatThrownBy(() -> payment(PaymentMethod.TARJETA_CREDITO, PaymentStatus.PENDIENTE).ensureRetryable())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void transferReferenceCanBeReplacedWhilePending() {
        Payment payment = payment(PaymentMethod.TRANSFERENCIA, PaymentStatus.PENDIENTE);

        payment.attachTransferReference("TRX-1");
        payment.attachTransferReference("TRX-2");

        assertThat(payment.getTransferReference()).isEqualTo("TRX-2");
    }

    @Test
    void transferReferenceOnlyAppliesToBankTransfers() {
        Payment payment = payment(PaymentMethod.TARJETA_DEBITO, PaymentStatus.PENDIENTE);

        assertThatThrownBy(() -> payment.attachTransferReference("TRX-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Payment payment(PaymentMethod method, PaymentStatus status) {
        return new Payment(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, method, status, null, null, null);
    }
}
