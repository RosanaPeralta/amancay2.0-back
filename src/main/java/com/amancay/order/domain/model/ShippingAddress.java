package com.amancay.order.domain.model;

// Copia de la direccion elegida al momento de confirmar el pedido: no se referencia
// en vivo a Address porque el usuario puede editarla o borrarla despues, y el pedido
// tiene que conservar los datos que se usaron para el envio.
public record ShippingAddress(String street, String number, String floorApt, String city, String province,
        String country, String postalCode) {
}
