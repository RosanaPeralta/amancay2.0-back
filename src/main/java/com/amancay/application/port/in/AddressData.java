package com.amancay.application.port.in;

public record AddressData(String street, Integer number, String floorApt, String city, String province,
        String country, String postalCode) {
}
