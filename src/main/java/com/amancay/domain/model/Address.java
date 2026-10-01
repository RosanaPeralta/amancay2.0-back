package com.amancay.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

/**
 * Dirección de envío de un usuario. Igual que {@link Review}, una nueva solo se construye por
 * la factory estática y valida sus propios campos: obligatorios calle, altura, localidad y país.
 * La predeterminada la administra el caso de uso; acá solo se marca y desmarca.
 */
@Getter
public class Address {

    private static final int MAX_STREET_LENGTH = 255;
    private static final int MAX_FLOOR_APT_LENGTH = 20;
    private static final int MAX_CITY_LENGTH = 120;
    private static final int MAX_PROVINCE_LENGTH = 120;
    private static final int MAX_COUNTRY_LENGTH = 120;
    private static final int MAX_POSTAL_CODE_LENGTH = 20;

    private final UUID id;
    private final UUID userId;
    private String street;
    private Integer number;
    private String floorApt;
    private String city;
    private String province;
    private String country;
    private String postalCode;
    private boolean defaultAddress;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Reconstruye una direccion ya existente (lo usa el adaptador de persistencia).
    public Address(UUID id, UUID userId, String street, Integer number, String floorApt, String city,
            String province, String country, String postalCode, boolean defaultAddress, Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.street = street;
        this.number = number;
        this.floorApt = floorApt;
        this.city = city;
        this.province = province;
        this.country = country;
        this.postalCode = postalCode;
        this.defaultAddress = defaultAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Address create(UUID userId, String street, Integer number, String floorApt, String city,
            String province, String country, String postalCode) {
        Address address = new Address(UUID.randomUUID(), Objects.requireNonNull(userId, "userId is required"),
                null, null, null, null, null, null, null, false, null, null);
        address.update(street, number, floorApt, city, province, country, postalCode);
        return address;
    }

    public void update(String street, Integer number, String floorApt, String city, String province, String country,
            String postalCode) {
        this.street = requireText(street, "street", MAX_STREET_LENGTH);
        this.number = requirePositive(number, "number");
        this.floorApt = optionalText(floorApt, "floorApt", MAX_FLOOR_APT_LENGTH);
        this.city = requireText(city, "city", MAX_CITY_LENGTH);
        this.province = optionalText(province, "province", MAX_PROVINCE_LENGTH);
        this.country = requireText(country, "country", MAX_COUNTRY_LENGTH);
        this.postalCode = optionalText(postalCode, "postalCode", MAX_POSTAL_CODE_LENGTH);
    }

    public void markDefault() {
        this.defaultAddress = true;
    }

    public void clearDefault() {
        this.defaultAddress = false;
    }

    private static Integer requirePositive(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive, but was " + value);
        }
        return value;
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
            throw new IllegalArgumentException(
                    field + " must be at most " + maxLength + " characters, but was " + trimmed.length());
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
