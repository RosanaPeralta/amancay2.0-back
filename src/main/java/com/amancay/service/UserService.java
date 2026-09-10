package com.amancay.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.UpdateUserRequest;
import com.amancay.dto.UserDto;
import com.amancay.entity.Role;
import com.amancay.entity.User;
import com.amancay.exceptions.UserNotFoundException;
import com.amancay.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDto getOrProvision(UUID id, String email, String name) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setName(name);
            user.setRole(Role.BUYER);
            user.setActive(true);
            return toDto(userRepository.save(user));
        }
        User user = existing.get();
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            return toDto(userRepository.save(user));
        }
        return toDto(user);
    }

    /**
     * Returns the role of the given user, provisioning the row on first sight so an authenticated
     * caller always resolves to a role.
     */
    @Transactional
    public Role getOrProvisionRole(UUID id, String email) {
        return getOrProvision(id, email, null).role();
    }

    @Transactional
    public UserDto updateProfile(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        user.setName(request.name());
        return toDto(userRepository.save(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
