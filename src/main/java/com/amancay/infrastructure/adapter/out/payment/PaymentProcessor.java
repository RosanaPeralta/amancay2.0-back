package com.amancay.infrastructure.adapter.out.payment;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentResult;

// Una estrategia de cobro (patron Strategy). PaymentProcessorRouter elige cual usar
// segun el metodo de pago.
interface PaymentProcessor {
    PaymentResult process(PayableOrder order, PaymentDetails details);
}
