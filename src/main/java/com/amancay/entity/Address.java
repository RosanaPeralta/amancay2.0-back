package com.amancay.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Dirección de envío de un usuario (USR-05/06). Igual que {@link Review}, solo se construye por
 * la factory estática y valida sus propios campos: obligatorios calle, altura, localidad y país.
 * La predeterminada la administra {@code AddressService}; acá solo se marca y desmarca.
 */
@Entity
@Table(name = "addresses")
@Getter
public class Address {

    private static final int MAX_STREET_LENGTH = 255;
    private static final int MAX_NUMBER_LENGTH = 20;
    private static final int MAX_FLOOR_APT_LENGTH = 50;
    private static final int MAX_CITY_LENGTH = 120;
    private static final int MAX_PROVINCE_LENGTH = 120;
    private static final int MAX_COUNTRY_LENGTH = 120;
    private static final int MAX_POSTAL_CODE_LENGTH = 20;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = MAX_STREET_LENGTH)
    private String street;

    @Column(nullable = false, length = MAX_NUMBER_LENGTH)
    private String number;

    @Column(name = "floor_apt", length = MAX_FLOOR_APT_LENGTH)
    private String floorApt;

    @Column(nullable = false, length = MAX_CITY_LENGTH)
    private String city;

    @Column(length = MAX_PROVINCE_LENGTH)
    private String province;

    @Column(nullable = false, length = MAX_COUNTRY_LENGTH)
    private String country;

    @Column(name = "postal_code", length = MAX_POSTAL_CODE_LENGTH)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Address() {
    }

    public static Address create(UUID userId, String street, String number, String floorApt, String city,
            String province, String country, String postalCode) {
        Address address = new Address();
        address.id = UUID.randomUUID();
        address.userId = Objects.requireNonNull(userId, "userId is required");
        address.defaultAddress = false;
        address.edit(street, number, floorApt, city, province, country, postalCode);
        return address;
    }

    public void edit(String street, String number, String floorApt, String city, String province, String country,
            String postalCode) {
        this.street = requireText(street, "street", MAX_STREET_LENGTH);
        this.number = requireText(number, "number", MAX_NUMBER_LENGTH);
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
