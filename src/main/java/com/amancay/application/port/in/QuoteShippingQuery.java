package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.ShippingQuote;

// Cotizacion previa al pedido, para mostrar el envio en el checkout.
public interface QuoteShippingQuery {
    ShippingQuote quote(UUID shippingAddressId);
}
