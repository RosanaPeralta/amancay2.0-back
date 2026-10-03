package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.UserRepositoryPort;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.User;
import com.amancay.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.mapper.PageMapper;
import com.amancay.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

@Component
class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;
    private final UserPersistenceMapper mapper;

    UserRepositoryAdapter(SpringDataUserRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void insertIfAbsent(UUID id, String email, String name) {
        repository.insertIfAbsent(id, email, name);
    }

    @Override
    public Optional<User> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<User> findAll(PageQuery page) {
        return PageMapper.toPageResult(repository.findAll(PageMapper.toPageable(page, PageMapper.NEWEST_FIRST)),
                mapper::toDomain);
    }

    @Override
    public PageResult<User> search(String text, PageQuery page) {
        return PageMapper.toPageResult(repository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(text,
                text, PageMapper.toPageable(page, PageMapper.NEWEST_FIRST)), mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = repository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(user.getId()));
        mapper.copyToEntity(user, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }
}
