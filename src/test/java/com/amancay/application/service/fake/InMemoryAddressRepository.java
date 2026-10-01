package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Address;
import com.amancay.domain.port.AddressRepositoryPort;

public class InMemoryAddressRepository implements AddressRepositoryPort {

    private final Map<UUID, Address> store = new LinkedHashMap<>();
    // Comparte el log con el fake de usuarios para poder verificar el orden lock -> count -> save.
    private final List<String> calls;
    private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

    public InMemoryAddressRepository(List<String> calls) {
        this.calls = calls;
    }

    public InMemoryAddressRepository() {
        this(new ArrayList<>());
    }

    public Address stored(UUID id) {
        return store.get(id);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryAddressRepository::copy);
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return store.values().stream().filter(a -> a.getUserId().equals(userId))
                .sorted(Comparator.comparing(Address::getCreatedAt)).map(InMemoryAddressRepository::copy).toList();
    }

    @Override
    public long countByUserId(UUID userId) {
        calls.add("count");
        return store.values().stream().filter(a -> a.getUserId().equals(userId)).count();
    }

    @Override
    public Optional<Address> findByIdAndUserId(UUID id, UUID userId) {
        return Optional.ofNullable(store.get(id)).filter(a -> a.getUserId().equals(userId))
                .map(InMemoryAddressRepository::copy);
    }

    @Override
    public void clearDefaultByUserId(UUID userId) {
        calls.add("clearDefault");
        store.values().stream().filter(a -> a.getUserId().equals(userId)).forEach(Address::clearDefault);
    }

    @Override
    public Address save(Address address) {
        calls.add("save");
        clock = clock.plusSeconds(1);
        Address saved = new Address(address.getId(), address.getUserId(), address.getStreet(), address.getNumber(),
                address.getFloorApt(), address.getCity(), address.getProvince(), address.getCountry(),
                address.getPostalCode(), address.isDefaultAddress(),
                address.getCreatedAt() == null ? clock : address.getCreatedAt(), clock);
        store.put(saved.getId(), saved);
        return copy(saved);
    }

    @Override
    public void delete(Address address) {
        calls.add("delete");
        store.remove(address.getId());
    }

    private static Address copy(Address a) {
        return new Address(a.getId(), a.getUserId(), a.getStreet(), a.getNumber(), a.getFloorApt(), a.getCity(),
                a.getProvince(), a.getCountry(), a.getPostalCode(), a.isDefaultAddress(), a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
