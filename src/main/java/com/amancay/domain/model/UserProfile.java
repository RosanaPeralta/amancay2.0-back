package com.amancay.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(UUID id, String email, String name, UserRole role, boolean active, Instant createdAt) {
}