package com.amancay.domain.model;

// Snapshot de la direccion al confirmar el pedido.
public record ShippingAddress(String street, String number, String floorApt, String city, String province,
        String country, String postalCode) {
}
