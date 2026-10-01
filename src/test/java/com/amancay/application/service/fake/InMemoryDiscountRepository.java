package com.amancay.application.service.fake;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amancay.domain.model.Discount;
import com.amancay.domain.port.DiscountRepositoryPort;

public class InMemoryDiscountRepository implements DiscountRepositoryPort {

    private final Map<Long, Discount> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public List<Discount> findAll() {
        return store.values().stream().map(InMemoryDiscountRepository::copy).toList();
    }

    @Override
    public Optional<Discount> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryDiscountRepository::copy);
    }

    @Override
    public List<Discount> findByDescription(String description) {
        return store.values().stream().filter(discount -> discount.getDescription().equals(description))
                .map(InMemoryDiscountRepository::copy).toList();
    }

    @Override
    public Discount save(Discount discount) {
        Long id = discount.getId() == null ? ++sequence : discount.getId();
        Discount saved = new Discount(id, discount.getPercentage(), discount.getDescription());
        store.put(id, saved);
        return copy(saved);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    private static Discount copy(Discount discount) {
        return new Discount(discount.getId(), discount.getPercentage(), discount.getDescription());
    }
}
