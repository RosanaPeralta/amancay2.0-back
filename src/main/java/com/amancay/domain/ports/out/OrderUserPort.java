package com.amancay.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.UserProfile;

public interface OrderUserPort {
    Optional<UserProfile> findUserById(UUID id);
}