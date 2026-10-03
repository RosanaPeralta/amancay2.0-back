package com.amancay.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.amancay.domain.model.User;
import com.amancay.infrastructure.adapter.out.persistence.entity.UserJpaEntity;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getName(), entity.getRole(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // El alta la hace insertIfAbsent; aca solo se copia lo que un usuario existente puede cambiar.
    public void copyToEntity(User user, UserJpaEntity entity) {
        entity.setEmail(user.getEmail());
        entity.setName(user.getName());
        entity.setRole(user.getRole());
    }
}
