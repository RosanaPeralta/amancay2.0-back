package com.amancay.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.Payment;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentStatus;

class PaymentRejectedMailContentTest {

    private final PaymentRejectedMailContent mailContent = new PaymentRejectedMailContent();

    @Test
    void explainsTheRejectionAndAsksToRetry() {
        UUID orderId = UUID.randomUUID();
        PayableOrder order = new PayableOrder(orderId, UUID.randomUUID(), new BigDecimal("150.00"), List.of());
        Payment payment = new Payment(UUID.randomUUID(), orderId, new BigDecimal("150.00"),
                PaymentMethod.TARJETA_CREDITO, PaymentStatus.RECHAZADO, null, null, null);

        MailContent content = mailContent.build(order, payment);

        assertThat(content.subject()).contains(orderId.toString());
        assertThat(content.body()).contains("150.00", "TARJETA_CREDITO", "reintentes el pago");
    }
}
