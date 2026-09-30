package com.amancay.payment.adapter.out.processor;

import com.amancay.payment.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentResult;

// Una estrategia de cobro (patron Strategy). PaymentProcessorRouter elige cual usar
// segun el metodo de pago.
interface PaymentProcessor {
    PaymentResult process(PayableOrder order, PaymentDetails details);
}
