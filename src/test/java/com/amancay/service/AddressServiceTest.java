package com.amancay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amancay.dto.AddressDto;
import com.amancay.dto.CreateAddressRequest;
import com.amancay.dto.UpdateAddressRequest;
import com.amancay.entity.Address;
import com.amancay.exceptions.AddressLimitReachedException;
import com.amancay.exceptions.AddressNotFoundException;
import com.amancay.repository.AddressRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADDRESS_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final CreateAddressRequest CREATE = new CreateAddressRequest("Calle", "1", null, "Ciudad", null,
            "País", null);

    @Mock
    private AddressRepository addressRepository;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository);
    }

    @Test
    void firstAddressBecomesDefaultAutomatically() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressDto result = addressService.create(USER_ID, CREATE);

        assertThat(result.id()).isNotNull();
        assertThat(result.street()).isEqualTo("Calle");
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void laterAddressesAreNotDefault() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressDto result = addressService.create(USER_ID, CREATE);

        assertThat(result.isDefault()).isFalse();
    }

    @Test
    void rejectsTheEleventhAddress() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn((long) AddressService.MAX_ADDRESSES_PER_USER);

        assertThatThrownBy(() -> addressService.create(USER_ID, CREATE))
                .isInstanceOf(AddressLimitReachedException.class);
        verify(addressRepository, never()).saveAndFlush(any(Address.class));
    }

    @Test
    void listsOwnAddresses() {
        when(addressRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(address()));

        List<AddressDto> result = addressService.list(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().city()).isEqualTo("Ciudad");
    }

    @Test
    void updatesOwnAddress() {
        Address existing = address();
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressDto result = addressService.update(USER_ID, ADDRESS_ID,
                new UpdateAddressRequest("Otra", "2", "1 A", "Otra ciudad", "Prov", "País", "1000"));

        assertThat(result.street()).isEqualTo("Otra");
        assertThat(result.floorApt()).isEqualTo("1 A");
    }

    @Test
    void addressOfAnotherUserIsNotFound() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update(USER_ID, ADDRESS_ID,
                new UpdateAddressRequest("Otra", "2", null, "C", null, "P", null)))
                .isInstanceOf(AddressNotFoundException.class);
        assertThatThrownBy(() -> addressService.delete(USER_ID, ADDRESS_ID))
                .isInstanceOf(AddressNotFoundException.class);
        verify(addressRepository, never()).saveAndFlush(any(Address.class));
        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    void deletesOwnAddress() {
        Address existing = address();
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(existing));

        addressService.delete(USER_ID, ADDRESS_ID);

        verify(addressRepository).delete(existing);
    }

    @Test
    void setDefaultClearsTheCurrentOneBeforeMarkingTheNew() {
        Address target = address();
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(target));
        when(addressRepository.saveAndFlush(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressDto result = addressService.setDefault(USER_ID, ADDRESS_ID);

        assertThat(result.isDefault()).isTrue();
        InOrder inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).clearDefaultByUserId(USER_ID);
        inOrder.verify(addressRepository).saveAndFlush(target);
    }

    @Test
    void setDefaultOnUnknownAddressIsNotFound() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.setDefault(USER_ID, ADDRESS_ID))
                .isInstanceOf(AddressNotFoundException.class);
        verify(addressRepository, never()).saveAndFlush(any(Address.class));
    }

    private Address address() {
        return Address.create(USER_ID, "Calle", "1", null, "Ciudad", null, "País", null);
    }
}
