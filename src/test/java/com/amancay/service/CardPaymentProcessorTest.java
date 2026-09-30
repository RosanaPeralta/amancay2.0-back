package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.amancay.dto.CreatePaymentRequest;
import com.amancay.entity.PaymentMethod;
import com.amancay.entity.PaymentStatus;
import com.amancay.order.domain.model.OrderFixtures;

class CardPaymentProcessorTest {

    private final CardPaymentProcessor processor = new CardPaymentProcessor();

    @Test
    void approvesACardNotEndingInTheSimulatedDeclineSuffix() {
        PaymentResult result = processor.process(OrderFixtures.newOrder(), cardRequest("4111111111111234"));

        assertThat(result.status()).isEqualTo(PaymentStatus.APROBADO);
        assertThat(result.reason()).isNull();
    }

    @Test
    void rejectsACardEndingInTheSimulatedDeclineSuffix() {
        PaymentResult result = processor.process(OrderFixtures.newOrder(), cardRequest("4111111111110000"));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(result.reason()).isNotBlank();
    }

    @Test
    void ignoresSpacesInTheCardNumberWhenCheckingTheSuffix() {
        PaymentResult result = processor.process(OrderFixtures.newOrder(), cardRequest("4111 1111 1111 0000"));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void rejectsWhenNoCardDataIsProvided() {
        CreatePaymentRequest request = new CreatePaymentRequest(PaymentMethod.TARJETA_CREDITO, null);

        PaymentResult result = processor.process(OrderFixtures.newOrder(), request);

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void rejectsWhenTheCardNumberIsBlank() {
        PaymentResult result = processor.process(OrderFixtures.newOrder(), cardRequest("   "));

        assertThat(result.status()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    private CreatePaymentRequest cardRequest(String number) {
        return new CreatePaymentRequest(PaymentMethod.TARJETA_CREDITO,
                new CreatePaymentRequest.CardData(number, "Test User", "12/30", "123"));
    }
}
