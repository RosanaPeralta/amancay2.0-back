package com.amancay.domain.ports.in;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.UserPage;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.UserRole;

public interface UserUseCases {
    UserProfile getOrProvision(UUID id, String email, String name);

    UserProfile updateProfile(UUID id, String name);

    UserPage listUsers(String query, int page, int size, List<SortOrder> sort);

    UserProfile changeRole(UUID adminId, UUID targetId, UserRole role);

    record SortOrder(String property, boolean ascending) {
    }
}