package com.amancay.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.port.in.ChangeUserRoleUseCase;
import com.amancay.application.port.in.GetOrProvisionUserUseCase;
import com.amancay.application.port.in.ListUsersQuery;
import com.amancay.application.port.in.UpdateProfileUseCase;
import com.amancay.application.port.out.UserRepositoryPort;
import com.amancay.domain.exception.InactiveUserException;
import com.amancay.domain.exception.SelfRoleChangeException;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.Role;
import com.amancay.domain.model.User;

@Service
public class UserService implements GetOrProvisionUserUseCase, UpdateProfileUseCase, ListUsersQuery,
        ChangeUserRoleUseCase {

    private final UserRepositoryPort userRepository;
    private final AdminGuard adminGuard;

    public UserService(UserRepositoryPort userRepository, AdminGuard adminGuard) {
        this.userRepository = userRepository;
        this.adminGuard = adminGuard;
    }

    @Override
    @Transactional
    public User getOrProvision(UUID id, String email, String name) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            userRepository.insertIfAbsent(id, email, name);
            existing = Optional.of(findUser(id));
        }
        User user = existing.get();
        if (!user.isActive()) {
            throw new InactiveUserException();
        }
        return user.syncWithToken(email, name) ? userRepository.save(user) : user;
    }

    @Override
    @Transactional
    public User updateName(UUID id, String name) {
        User user = findUser(id);
        user.rename(name);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> list(UUID requesterId, String text, PageQuery page) {
        adminGuard.requireAdmin(requesterId);
        return text == null || text.isBlank() ? userRepository.findAll(page)
                : userRepository.search(text.trim(), page);
    }

    @Override
    @Transactional
    public User changeRole(UUID adminId, UUID targetId, Role role) {
        adminGuard.requireAdmin(adminId);
        if (adminId.equals(targetId) && role != Role.ADMIN) {
            throw new SelfRoleChangeException();
        }
        User user = findUser(targetId);
        user.changeRole(role);
        return userRepository.save(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
