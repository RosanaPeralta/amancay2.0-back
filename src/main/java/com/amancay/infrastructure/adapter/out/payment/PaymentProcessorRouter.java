package com.amancay.infrastructure.adapter.out.payment;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.application.port.out.PaymentProcessorPort;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentMethod;
import com.amancay.domain.model.PaymentResult;

// Delega en la estrategia de cobro segun el metodo de pago.
@Component
class PaymentProcessorRouter implements PaymentProcessorPort {

    private final Map<PaymentMethod, PaymentProcessor> processorsByMethod;

    PaymentProcessorRouter(CardPaymentProcessor cardPaymentProcessor,
            ManualConfirmationPaymentProcessor manualConfirmationPaymentProcessor) {
        this.processorsByMethod = Map.of(
                PaymentMethod.TARJETA_CREDITO, cardPaymentProcessor,
                PaymentMethod.TARJETA_DEBITO, cardPaymentProcessor,
                PaymentMethod.TRANSFERENCIA, manualConfirmationPaymentProcessor);
    }

    @Override
    public PaymentResult process(PayableOrder order, PaymentDetails details) {
        PaymentProcessor processor = processorsByMethod.get(details.method());
        if (processor == null) {
            throw new IllegalStateException("No payment processor configured for method " + details.method());
        }
        return processor.process(order, details);
    }
}
