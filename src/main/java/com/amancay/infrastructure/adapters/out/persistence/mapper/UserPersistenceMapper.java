package com.amancay.infrastructure.adapters.out.persistence.mapper;

import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.UserRole;

public final class UserPersistenceMapper {
    private UserPersistenceMapper() {
    }

    public static UserProfile toDomain(com.amancay.entity.User entity) {
        return new UserProfile(entity.getId(), entity.getEmail(), entity.getName(), UserRole.valueOf(entity.getRole().name()),
                entity.isActive(), entity.getCreatedAt());
    }

    public static com.amancay.entity.User toPersistence(UserProfile user, com.amancay.entity.User entity) {
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setName(user.name());
        entity.setRole(com.amancay.entity.Role.valueOf(user.role().name()));
        entity.setActive(user.active());
        return entity;
    }
}