package com.amancay.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.amancay.domain.model.Role;
import com.amancay.domain.model.User;

public record UserResponse(
        UUID id,
        String email,
        String name,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
