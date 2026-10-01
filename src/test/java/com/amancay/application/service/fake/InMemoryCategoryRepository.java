package com.amancay.application.service.fake;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.Category;
import com.amancay.domain.port.CategoryRepositoryPort;

public class InMemoryCategoryRepository implements CategoryRepositoryPort {

    private final Map<UUID, Category> store = new LinkedHashMap<>();

    @Override
    public List<Category> findAll() {
        return store.values().stream().map(c -> new Category(c.getId(), c.getName())).toList();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(store.get(id)).map(c -> new Category(c.getId(), c.getName()));
    }

    @Override
    public boolean existsByName(String name) {
        return store.values().stream().anyMatch(c -> c.getName().equals(name));
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return store.values().stream().anyMatch(c -> c.getName().equals(name) && !c.getId().equals(id));
    }

    @Override
    public Category save(Category category) {
        UUID id = category.getId() == null ? UUID.randomUUID() : category.getId();
        store.put(id, new Category(id, category.getName()));
        return new Category(id, category.getName());
    }

    @Override
    public void deleteById(UUID id) {
        store.remove(id);
    }
}
