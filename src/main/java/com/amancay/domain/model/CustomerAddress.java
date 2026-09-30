package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record CustomerAddress(UUID id, UUID userId, String street, Integer number, String floorApt, String city,
        String province, String country, String postalCode, boolean defaultAddress, Instant createdAt, Instant updatedAt) {
    private static final int MAX_STREET_LENGTH = 255;
    private static final int MAX_CITY_LENGTH = 120;
    private static final int MAX_PROVINCE_LENGTH = 120;
    private static final int MAX_COUNTRY_LENGTH = 120;
    private static final int MAX_POSTAL_CODE_LENGTH = 20;

    public static CustomerAddress create(UUID userId, String street, Integer number, String floorApt, String city,
            String province, String country, String postalCode) {
        if (userId == null) {
            throw new NullPointerException("userId is required");
        }
        return new CustomerAddress(UUID.randomUUID(), userId, street, number, floorApt, city, province, country,
                postalCode, false, null, null).validated();
    }

    public CustomerAddress update(String street, Integer number, String floorApt, String city, String province,
            String country, String postalCode) {
        return new CustomerAddress(id, userId, street, number, floorApt, city, province, country, postalCode,
                defaultAddress, createdAt, updatedAt).validated();
    }

    public CustomerAddress withDefault(boolean value) {
        return new CustomerAddress(id, userId, street, number, floorApt, city, province, country, postalCode,
                value, createdAt, updatedAt);
    }

    private CustomerAddress validated() {
        if (number == null) {
            throw new IllegalArgumentException("number is required");
        }
        if (number <= 0) {
            throw new IllegalArgumentException("number must be positive, but was " + number);
        }
        return new CustomerAddress(id, userId, requireText(street, "street", MAX_STREET_LENGTH), number,
                optionalText(floorApt, "floorApt", 20), requireText(city, "city", MAX_CITY_LENGTH),
                optionalText(province, "province", MAX_PROVINCE_LENGTH),
                requireText(country, "country", MAX_COUNTRY_LENGTH),
                optionalText(postalCode, "postalCode", MAX_POSTAL_CODE_LENGTH), defaultAddress, createdAt, updatedAt);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return optionalText(value, field, maxLength);
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters, but was "
                    + trimmed.length());
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}