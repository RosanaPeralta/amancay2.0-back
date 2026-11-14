package com.amancay.infrastructure.adapter.out.mercadopago;

import com.amancay.application.port.out.PayableOrderPort.PayableOrder;

// Crea el checkout (preferencia de Checkout Pro) de una orden y devuelve la URL a la que
// hay que redirigir al comprador.
public interface MercadoPagoCheckout {
    String createCheckout(PayableOrder order);
}
