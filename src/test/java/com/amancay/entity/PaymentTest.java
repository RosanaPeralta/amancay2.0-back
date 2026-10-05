package com.amancay.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.amancay.exceptions.InvalidPaymentStatusTransitionException;

class PaymentTest {

    @Test
    void newPaymentStartsAsPendiente() {
        Payment payment = new Payment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDIENTE);
    }

    @Test
    void changeStatusAllowsPendienteToAprobado() {
        Payment payment = new Payment();

        payment.changeStatus(PaymentStatus.APROBADO);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APROBADO);
    }

    @Test
    void changeStatusAllowsPendienteToRechazado() {
        Payment payment = new Payment();

        payment.changeStatus(PaymentStatus.RECHAZADO);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void aprobadoIsATerminalState() {
        Payment payment = new Payment();
        payment.changeStatus(PaymentStatus.APROBADO);

        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.RECHAZADO))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APROBADO);
    }

    @Test
    void rechazadoIsATerminalState() {
        Payment payment = new Payment();
        payment.changeStatus(PaymentStatus.RECHAZADO);

        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.APROBADO))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECHAZADO);
    }
}
