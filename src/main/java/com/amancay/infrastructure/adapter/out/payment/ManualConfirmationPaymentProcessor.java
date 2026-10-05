package com.amancay.infrastructure.adapter.out.payment;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentResult;

// Transferencia: queda PENDIENTE hasta que el admin la confirma a mano.
@Component
class ManualConfirmationPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(PayableOrder order, PaymentDetails details) {
        return PaymentResult.pending("Awaiting manual confirmation (bank transfer)");
    }
}
