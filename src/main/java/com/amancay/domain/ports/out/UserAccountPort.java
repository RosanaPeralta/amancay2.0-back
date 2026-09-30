package com.amancay.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.UserPage;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.ports.in.UserUseCases.SortOrder;

public interface UserAccountPort {
    Optional<UserProfile> findById(UUID id);

    int insertIfAbsent(UUID id, String email, String name);

    UserProfile save(UserProfile user);

    UserPage search(String query, int page, int size, java.util.List<SortOrder> sort);
}