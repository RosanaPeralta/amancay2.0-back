package com.amancay.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Columnas shipping_* de orders; el equivalente de dominio es ShippingAddress.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddressEmbeddable {

    @Column(name = "shipping_street", nullable = false)
    private String street;

    @Column(name = "shipping_number", nullable = false)
    private String number;

    @Column(name = "shipping_floor_apt")
    private String floorApt;

    @Column(name = "shipping_city", nullable = false)
    private String city;

    @Column(name = "shipping_province")
    private String province;

    @Column(name = "shipping_country", nullable = false)
    private String country;

    @Column(name = "shipping_postal_code")
    private String postalCode;
}
