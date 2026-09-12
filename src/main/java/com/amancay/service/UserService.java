package com.amancay.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.dto.PageResponse;
import com.amancay.dto.UpdateUserRequest;
import com.amancay.dto.UserDto;
import com.amancay.entity.Role;
import com.amancay.entity.User;
import com.amancay.exceptions.SelfRoleChangeException;
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
            return toDto(userRepository.saveAndFlush(user));
        }
        User user = existing.get();
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            return toDto(userRepository.saveAndFlush(user));
        }
        return toDto(user);
    }

    @Transactional
    public Role getOrProvisionRole(UUID id, String email) {
        return getOrProvision(id, email, null).role();
    }

    @Transactional
    public UserDto updateProfile(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        user.setName(request.name());
        return toDto(userRepository.saveAndFlush(user));
    }

    // --- Administración (USR-09) ----------------------------------------------------------

    /** Búsqueda por email o nombre; con {@code q} vacío lista todos. */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(String q, Pageable pageable) {
        Page<User> users = (q == null || q.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(q.trim(), q.trim(),
                        pageable);
        return new PageResponse<>(users.map(this::toDto).getContent(), users.getNumber(), users.getSize(),
                users.getTotalElements(), users.getTotalPages());
    }

    /** Un ADMIN no puede quitarse su propio rol: evita dejar el sistema sin administradores por error. */
    @Transactional
    public UserDto changeRole(UUID adminId, UUID targetId, Role role) {
        if (adminId.equals(targetId) && role != Role.ADMIN) {
            throw new SelfRoleChangeException();
        }
        User user = findUser(targetId);
        user.setRole(role);
        return toDto(userRepository.saveAndFlush(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
