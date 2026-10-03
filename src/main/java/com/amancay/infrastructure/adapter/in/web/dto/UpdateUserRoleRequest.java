package com.amancay.infrastructure.adapter.in.web.dto;

import com.amancay.domain.model.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role) {
}
