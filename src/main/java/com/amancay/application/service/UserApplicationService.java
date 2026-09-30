package com.amancay.application.service;

import java.util.List;
import java.util.UUID;

import com.amancay.domain.model.UserPage;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.UserRole;
import com.amancay.domain.ports.in.UserUseCases;
import com.amancay.domain.ports.out.UserAccountPort;
import com.amancay.exceptions.InactiveUserException;
import com.amancay.exceptions.SelfRoleChangeException;
import com.amancay.exceptions.UserNotFoundException;

public class UserApplicationService implements UserUseCases {
    private final UserAccountPort userAccount;

    public UserApplicationService(UserAccountPort userAccount) {
        this.userAccount = userAccount;
    }

    @Override
    public UserProfile getOrProvision(UUID id, String email, String name) {
        UserProfile user = userAccount.findById(id).orElse(null);
        if (user == null) {
            userAccount.insertIfAbsent(id, email, name);
            user = findUser(id);
        }
        if (!user.active()) {
            throw new InactiveUserException();
        }
        String synchronizedEmail = email != null && !email.equals(user.email()) ? email : user.email();
        String synchronizedName = name != null && user.name() == null ? name : user.name();
        if (!java.util.Objects.equals(synchronizedEmail, user.email())
                || !java.util.Objects.equals(synchronizedName, user.name())) {
            user = userAccount.save(new UserProfile(user.id(), synchronizedEmail, synchronizedName, user.role(),
                    user.active(), user.createdAt()));
        }
        return user;
    }

    @Override
    public UserProfile updateProfile(UUID id, String name) {
        UserProfile user = findUser(id);
        return userAccount.save(new UserProfile(user.id(), user.email(), name, user.role(), user.active(), user.createdAt()));
    }

    @Override
    public UserPage listUsers(String query, int page, int size, List<SortOrder> sort) {
        return userAccount.search(query, page, size, sort);
    }

    @Override
    public UserProfile changeRole(UUID adminId, UUID targetId, UserRole role) {
        if (adminId.equals(targetId) && role != UserRole.ADMIN) {
            throw new SelfRoleChangeException();
        }
        UserProfile user = findUser(targetId);
        return userAccount.save(new UserProfile(user.id(), user.email(), user.name(), role, user.active(), user.createdAt()));
    }

    private UserProfile findUser(UUID id) {
        return userAccount.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}