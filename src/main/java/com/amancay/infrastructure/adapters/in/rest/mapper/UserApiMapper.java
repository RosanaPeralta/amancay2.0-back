package com.amancay.infrastructure.adapters.in.rest.mapper;

import com.amancay.domain.model.UserProfile;
import com.amancay.dto.UserDto;

public final class UserApiMapper {
    private UserApiMapper() {
    }

    public static UserDto toDto(UserProfile user) {
        return new UserDto(user.id(), user.email(), user.name(), com.amancay.entity.Role.valueOf(user.role().name()),
                user.createdAt());
    }
}