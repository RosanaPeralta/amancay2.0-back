package com.amancay.domain.model;

public record ShippingAddressSnapshot(String street, String number, String floorApt, String city, String province,
        String country, String postalCode) {
}