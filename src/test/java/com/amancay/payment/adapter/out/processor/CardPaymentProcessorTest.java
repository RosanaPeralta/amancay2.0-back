package com.amancay.payment.adapter.out.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.payment.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentMethod;
import com.amancay.payment.domain.model.PaymentResult;
import com.amancay.payment.domain.model.PaymentStatus;

class CardPaymentProcessorTest {

    private static final PayableOrder ORDER = new PayableOrder(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN,
            List.of());

    private final CardPaymentProcessor processor = new CardPaymentProcessor();

    @Test
    void approvesACardNotEndingInTheSimulatedDeclineSuffix() {
        PaymentResult result = processor.process(ORDER, cardRequest("4111111111111234"));

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
    }

    @Test
    void rejectsACardEndingInTheSimulatedDeclineSuffix() {
        PaymentResult result = processor.process(ORDER, cardRequest("4111111111110000"));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void ignoresSpacesInTheCardNumberWhenCheckingTheSuffix() {
        PaymentResult result = processor.process(ORDER, cardRequest("4111 1111 1111 0000"));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void rejectsWhenNoCardDataIsProvided() {
        PaymentResult result = processor.process(ORDER, new PaymentDetails(PaymentMethod.TARJETA_CREDITO, null));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void rejectsWhenTheCardNumberIsBlank() {
        PaymentResult result = processor.process(ORDER, cardRequest("   "));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    private PaymentDetails cardRequest(String number) {
        return new PaymentDetails(PaymentMethod.TARJETA_CREDITO,
                new PaymentDetails.CardData(number, "Test User", "12/30", "123"));
    }
}
