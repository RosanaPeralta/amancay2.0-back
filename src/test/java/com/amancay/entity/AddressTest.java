package com.amancay.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Protege las invariantes de {@link Address#create}: los cuatro campos obligatorios del PDF de
 * requerimientos (calle, altura, localidad, país) y los largos de columna. Igual que en
 * {@link ReviewTest}, la duplicación con Bean Validation es deliberada: el DTO cuida el borde HTTP,
 * la factory cuida el dominio.
 */
class AddressTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void createBuildsACompleteNonDefaultAddress() {
        Address address = Address.create(USER_ID, "Av. Siempreviva", "742", "3 B", "Springfield", "Buenos Aires",
                "Argentina", "1234");

        assertThat(address.getId()).isNotNull();
        assertThat(address.getUserId()).isEqualTo(USER_ID);
        assertThat(address.getStreet()).isEqualTo("Av. Siempreviva");
        assertThat(address.getNumber()).isEqualTo("742");
        assertThat(address.getFloorApt()).isEqualTo("3 B");
        assertThat(address.getCity()).isEqualTo("Springfield");
        assertThat(address.getProvince()).isEqualTo("Buenos Aires");
        assertThat(address.getCountry()).isEqualTo("Argentina");
        assertThat(address.getPostalCode()).isEqualTo("1234");
        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void createTrimsAndTurnsBlankOptionalsIntoNull() {
        Address address = Address.create(USER_ID, "  Calle ", "1", "   ", "Ciudad", "", "País", null);

        assertThat(address.getStreet()).isEqualTo("Calle");
        assertThat(address.getFloorApt()).isNull();
        assertThat(address.getProvince()).isNull();
        assertThat(address.getPostalCode()).isNull();
    }

    @Test
    void createRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> Address.create(USER_ID, " ", "1", null, "Ciudad", null, "País", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("street");
        assertThatThrownBy(() -> Address.create(USER_ID, "Calle", null, null, "Ciudad", null, "País", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("number");
        assertThatThrownBy(() -> Address.create(USER_ID, "Calle", "1", null, "", null, "País", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("city");
        assertThatThrownBy(() -> Address.create(USER_ID, "Calle", "1", null, "Ciudad", null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("country");
    }

    @Test
    void createRejectsValuesLongerThanTheColumn() {
        assertThatThrownBy(() -> Address.create(USER_ID, "Calle", "x".repeat(21), null, "Ciudad", null, "País", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("number");
    }

    @Test
    void createRejectsAMissingUser() {
        assertThatThrownBy(() -> Address.create(null, "Calle", "1", null, "Ciudad", null, "País", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void editRevalidatesAndKeepsIdentityAndDefaultFlag() {
        Address address = Address.create(USER_ID, "Calle", "1", null, "Ciudad", null, "País", null);
        UUID id = address.getId();
        address.markDefault();

        address.edit("Otra", "2", "PB", "Otra ciudad", "Otra prov", "Otro país", "9999");

        assertThat(address.getId()).isEqualTo(id);
        assertThat(address.getStreet()).isEqualTo("Otra");
        assertThat(address.isDefaultAddress()).isTrue();
        assertThatThrownBy(() -> address.edit("", "2", null, "C", null, "P", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
