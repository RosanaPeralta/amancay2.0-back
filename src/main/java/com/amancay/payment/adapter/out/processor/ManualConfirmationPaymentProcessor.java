package com.amancay.payment.adapter.out.processor;

import org.springframework.stereotype.Component;

import com.amancay.payment.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentResult;

// Transferencia: queda PENDIENTE hasta que el admin la confirma a mano.
@Component
class ManualConfirmationPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(PayableOrder order, PaymentDetails details) {
        return PaymentResult.pending("Awaiting manual confirmation (bank transfer)");
    }
}
