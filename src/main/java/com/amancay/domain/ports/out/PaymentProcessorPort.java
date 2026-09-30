package com.amancay.domain.ports.out;

import com.amancay.domain.model.PaymentMethodType;
import com.amancay.domain.model.PaymentOutcome;
import com.amancay.domain.ports.in.PaymentUseCases.PaymentRequest;
import com.amancay.domain.model.PurchaseOrder;

public interface PaymentProcessorPort {
    PaymentOutcome process(PaymentMethodType method, PurchaseOrder order, PaymentRequest request);
}