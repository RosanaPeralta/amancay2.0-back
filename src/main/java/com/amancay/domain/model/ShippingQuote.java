package com.amancay.domain.model;

import java.math.BigDecimal;

// Resultado de cotizar un envio: distancia en ruta desde el deposito y su costo.
public record ShippingQuote(BigDecimal distanceKm, BigDecimal cost) {
}
