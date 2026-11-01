package com.amancay.application.port.out;

import com.amancay.domain.model.ShippingAddress;
import com.amancay.domain.model.ShippingQuote;

// Calcula distancia y costo del envio hasta la direccion. Lo implementa un servicio externo.
public interface ShippingQuotePort {
    ShippingQuote quote(ShippingAddress destination);
}
