package com.amancay.infrastructure.adapter.out.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentStatus;

class PaymentProcessorRouterTest {

    private static final PayableOrder ORDER = new PayableOrder(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN,
            List.of());
    private static final PaymentDetails.CardData CARD = new PaymentDetails.CardData("4111111111111234", "T", "12/30",
            "123");

    private final PaymentProcessorRouter router = new PaymentProcessorRouter(new CardPaymentProcessor(),
            new ManualConfirmationPaymentProcessor());

    @Test
    void cardMethodsGoToTheCardProcessor() {
        assertThat(router.process(ORDER, new PaymentDetails(PaymentMethod.TARJETA_CREDITO, CARD)).status())
                .isEqualTo(PaymentStatus.APROBADO);
        assertThat(router.process(ORDER, new PaymentDetails(PaymentMethod.TARJETA_DEBITO, CARD)).status())
                .isEqualTo(PaymentStatus.APROBADO);
    }

    @Test
    void bankTransferStaysPendingForManualConfirmation() {
        assertThat(router.process(ORDER, new PaymentDetails(PaymentMethod.TRANSFERENCIA, null)).status())
                .isEqualTo(PaymentStatus.PENDIENTE);
    }
}
