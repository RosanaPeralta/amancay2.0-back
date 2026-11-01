package com.amancay.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

import com.amancay.domain.model.ShippingQuote;

public record ShippingQuoteResponse(BigDecimal distanceKm, BigDecimal cost) {

    public static ShippingQuoteResponse from(ShippingQuote quote) {
        return new ShippingQuoteResponse(quote.distanceKm(), quote.cost());
    }
}
