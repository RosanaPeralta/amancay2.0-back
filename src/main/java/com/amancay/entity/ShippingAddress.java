package com.amancay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Copia de la direccion elegida al momento de confirmar el pedido: no se referencia
// en vivo a Address porque el usuario puede editarla o borrarla despues, y el pedido
// tiene que conservar los datos que se usaron para el envio.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress {

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
