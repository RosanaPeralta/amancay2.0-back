package com.amancay.domain.model;

// Datos con los que el comprador intenta pagar. card solo aplica a los metodos con
// tarjeta; cada estrategia de cobro valida lo que necesita.
public record PaymentDetails(PaymentMethod method, CardData card) {

    public record CardData(String number, String holderName, String expiry, String cvv) {
    }
}
