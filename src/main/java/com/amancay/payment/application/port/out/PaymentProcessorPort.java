package com.amancay.payment.application.port.out;

import com.amancay.payment.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.payment.domain.model.PaymentDetails;
import com.amancay.payment.domain.model.PaymentResult;

// El cobro en si (tarjeta, transferencia...). El adaptador elige la estrategia segun
// details.method().
public interface PaymentProcessorPort {
    PaymentResult process(PayableOrder order, PaymentDetails details);
}
