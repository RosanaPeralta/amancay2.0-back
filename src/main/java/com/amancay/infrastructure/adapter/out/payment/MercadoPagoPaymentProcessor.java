package com.amancay.infrastructure.adapter.out.payment;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;
import com.amancay.domain.model.PaymentDetails;
import com.amancay.domain.model.PaymentResult;
import com.amancay.infrastructure.adapter.out.mercadopago.MercadoPagoCheckout;

// Mercado Pago (Checkout Pro): el pago queda PENDIENTE con la URL del checkout. Lo aprueba
// el webhook de Mercado Pago o la verificacion que pide el comprador al volver.
@Component
class MercadoPagoPaymentProcessor implements PaymentProcessor {

    private final MercadoPagoCheckout checkout;

    MercadoPagoPaymentProcessor(MercadoPagoCheckout checkout) {
        this.checkout = checkout;
    }

    @Override
    public PaymentResult process(PayableOrder order, PaymentDetails details) {
        return PaymentResult.awaitingCheckout(checkout.createCheckout(order));
    }
}
