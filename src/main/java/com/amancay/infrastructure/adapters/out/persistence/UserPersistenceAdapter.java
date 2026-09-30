package com.amancay.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.domain.model.UserPage;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.ports.in.UserUseCases.SortOrder;
import com.amancay.domain.ports.out.UserAccountPort;
import com.amancay.infrastructure.adapters.out.persistence.mapper.UserPersistenceMapper;
import com.amancay.infrastructure.adapters.out.persistence.repository.UserRepository;

@Repository
public class UserPersistenceAdapter implements UserAccountPort {
    private final UserRepository repository;

    public UserPersistenceAdapter(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(UUID id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public int insertIfAbsent(UUID id, String email, String name) {
        return repository.insertIfAbsent(id, email, name);
    }

    @Override
    @Transactional
    public UserProfile save(UserProfile user) {
        com.amancay.entity.User entity = repository.findById(user.id()).orElseGet(com.amancay.entity.User::new);
        return UserPersistenceMapper.toDomain(repository.saveAndFlush(UserPersistenceMapper.toPersistence(user, entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserPage search(String query, int page, int size, List<SortOrder> sort) {
        List<Sort.Order> sortOrders = sort.stream()
                .map(order -> order.ascending() ? Sort.Order.asc(order.property()) : Sort.Order.desc(order.property()))
                .toList();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortOrders));
        Page<com.amancay.entity.User> users = query == null || query.isBlank()
                ? repository.findAll(pageable)
                : repository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(query.trim(), query.trim(), pageable);
        return new UserPage(users.getContent().stream().map(UserPersistenceMapper::toDomain).toList(), users.getNumber(),
                users.getSize(), users.getTotalElements(), users.getTotalPages());
    }
}