package com.amancay.application.service.fake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amancay.application.port.out.UserRepositoryPort;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Role;
import com.amancay.domain.model.User;

public class InMemoryUserRepository implements UserRepositoryPort {

    private final Map<UUID, User> store = new LinkedHashMap<>();
    public final List<String> calls = new ArrayList<>();
    private Instant clock = Instant.parse("2026-01-01T00:00:00Z");

    public User add(UUID id, String email, String name, Role role, boolean active) {
        clock = clock.plusSeconds(1);
        User user = new User(id, email, name, role, active, clock, clock);
        store.put(id, user);
        return copy(user);
    }

    public User stored(UUID id) {
        return store.get(id);
    }

    public long count(String call) {
        return calls.stream().filter(call::equals).count();
    }

    @Override
    public Optional<User> findById(UUID id) {
        calls.add("findById");
        return Optional.ofNullable(store.get(id)).map(InMemoryUserRepository::copy);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        calls.add("findAllById");
        return ids.stream().map(store::get).filter(user -> user != null).map(InMemoryUserRepository::copy).toList();
    }

    @Override
    public void insertIfAbsent(UUID id, String email, String name) {
        calls.add("insertIfAbsent");
        store.computeIfAbsent(id, key -> new User(key, email, name, Role.BUYER, true, clock, clock));
    }

    @Override
    public Optional<User> findByIdForUpdate(UUID id) {
        calls.add("lock");
        return Optional.ofNullable(store.get(id)).map(InMemoryUserRepository::copy);
    }

    @Override
    public PageResult<User> findAll(PageQuery page) {
        calls.add("findAll");
        return Pages.of(newestFirst(store.values()), page);
    }

    @Override
    public PageResult<User> search(String text, PageQuery page) {
        calls.add("search");
        String needle = text.toLowerCase(Locale.ROOT);
        return Pages.of(newestFirst(store.values().stream()
                .filter(user -> contains(user.getEmail(), needle) || contains(user.getName(), needle))
                .toList()), page);
    }

    @Override
    public User save(User user) {
        calls.add("save");
        store.put(user.getId(), copy(user));
        return copy(user);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<User> newestFirst(Collection<User> users) {
        return users.stream().sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(InMemoryUserRepository::copy).toList();
    }

    private static User copy(User user) {
        return new User(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
