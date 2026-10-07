package com.amancay.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amancay.application.port.in.AddressData;
import com.amancay.application.service.fake.InMemoryAddressRepository;
import com.amancay.application.service.fake.InMemoryUserRepository;
import com.amancay.domain.exception.AddressLimitReachedException;
import com.amancay.domain.exception.AddressNotFoundException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Address;
import com.amancay.domain.model.Role;

class AddressServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final AddressData DATA = new AddressData("Calle", 1, null, "Ciudad", null, "País", null);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    // Mismo log en ambos fakes, para verificar el orden de las operaciones.
    private final List<String> calls = users.calls;
    private final InMemoryAddressRepository addresses = new InMemoryAddressRepository(calls);
    private AddressService addressService;

    @BeforeEach
    void setUp() {
        users.create(USER_ID, "a@amancay.com", "Ada", Role.BUYER, true);
        addressService = new AddressService(addresses, users);
    }

    @Test
    void firstAddressBecomesDefaultAutomaticallyAfterLockingTheUser() {
        Address result = addressService.create(USER_ID, DATA);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStreet()).isEqualTo("Calle");
        assertThat(result.isDefaultAddress()).isTrue();
        assertThat(calls).containsSubsequence("lock", "count", "save");
    }

    @Test
    void laterAddressesAreNotDefault() {
        addressService.create(USER_ID, DATA);

        assertThat(addressService.create(USER_ID, DATA).isDefaultAddress()).isFalse();
    }

    @Test
    void rejectsTheEleventhAddress() {
        for (int i = 0; i < AddressService.MAX_ADDRESSES_PER_USER; i++) {
            addressService.create(USER_ID, DATA);
        }
        calls.clear();

        assertThatThrownBy(() -> addressService.create(USER_ID, DATA))
                .isInstanceOf(AddressLimitReachedException.class);
        assertThat(calls).doesNotContain("save");
    }

    @Test
    void creatingForAnUnknownUserIsUserNotFound() {
        assertThatThrownBy(() -> addressService.create(UUID.randomUUID(), DATA))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listsOwnAddresses() {
        addressService.create(USER_ID, DATA);

        List<Address> result = addressService.list(USER_ID);

        assertThat(result).singleElement().satisfies(address -> assertThat(address.getCity()).isEqualTo("Ciudad"));
    }

    @Test
    void updatesOwnAddress() {
        Address existing = addressService.create(USER_ID, DATA);

        Address result = addressService.update(USER_ID, existing.getId(),
                new AddressData("Otra", 2, "A", "Otra ciudad", "Prov", "País", "1000"));

        assertThat(result.getStreet()).isEqualTo("Otra");
        assertThat(result.getFloorApt()).isEqualTo("A");
        assertThat(addresses.stored(existing.getId()).getCity()).isEqualTo("Otra ciudad");
    }

    @Test
    void addressOfAnotherUserIsNotFound() {
        UUID otherUser = UUID.randomUUID();
        users.create(otherUser, "b@amancay.com", "Bob", Role.BUYER, true);
        Address theirs = addressService.create(otherUser, DATA);
        calls.clear();

        assertThatThrownBy(() -> addressService.update(USER_ID, theirs.getId(), DATA))
                .isInstanceOf(AddressNotFoundException.class);
        assertThatThrownBy(() -> addressService.delete(USER_ID, theirs.getId()))
                .isInstanceOf(AddressNotFoundException.class);
        assertThat(calls).doesNotContain("save", "delete");
    }

    @Test
    void deletesOwnAddress() {
        Address existing = addressService.create(USER_ID, DATA);

        addressService.delete(USER_ID, existing.getId());

        assertThat(addresses.stored(existing.getId())).isNull();
    }

    @Test
    void setDefaultClearsTheCurrentOneBeforeMarkingTheNew() {
        Address first = addressService.create(USER_ID, DATA);
        Address second = addressService.create(USER_ID, DATA);
        calls.clear();

        Address result = addressService.setDefault(USER_ID, second.getId());

        assertThat(result.isDefaultAddress()).isTrue();
        assertThat(addresses.stored(first.getId()).isDefaultAddress()).isFalse();
        assertThat(calls).containsSubsequence("lock", "clearDefault", "save");
    }

    @Test
    void setDefaultOnUnknownAddressIsNotFound() {
        assertThatThrownBy(() -> addressService.setDefault(USER_ID, UUID.randomUUID()))
                .isInstanceOf(AddressNotFoundException.class);
        assertThat(calls).doesNotContain("save");
    }

    @Test
    void createRejectsInvalidDataFromTheDomain() {
        assertThatThrownBy(() -> addressService.create(USER_ID,
                new AddressData(" ", 1, null, "Ciudad", null, "País", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("street");
    }
}
