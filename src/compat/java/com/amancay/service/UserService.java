package com.amancay.service;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amancay.application.service.UserApplicationService;
import com.amancay.domain.model.UserPage;
import com.amancay.domain.model.UserProfile;
import com.amancay.domain.model.UserRole;
import com.amancay.domain.ports.in.UserUseCases;
import com.amancay.domain.ports.in.UserUseCases.SortOrder;
import com.amancay.domain.ports.out.UserAccountPort;
import com.amancay.dto.PageResponse;
import com.amancay.dto.UpdateUserRequest;
import com.amancay.dto.UserDto;
import com.amancay.entity.Role;
import com.amancay.infrastructure.adapters.in.rest.mapper.UserApiMapper;
import com.amancay.infrastructure.adapters.out.persistence.UserPersistenceAdapter;
import com.amancay.repository.UserRepository;

@Service
public class UserService {
    private final UserUseCases userUseCases;
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserUseCases userUseCases, UserRepository userRepository) {
        this.userUseCases = userUseCases;
        this.userRepository = userRepository;
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        UserAccountPort adapter = new UserPersistenceAdapter(userRepository);
        this.userUseCases = new UserApplicationService(adapter);
    }

    @Transactional
    public UserDto getOrProvision(UUID id, String email, String name) {
        return UserApiMapper.toDto(userUseCases.getOrProvision(id, email, name));
    }

    @Transactional
    public Role getOrProvisionRole(UUID id, String email, String name) {
        return com.amancay.entity.Role.valueOf(userUseCases.getOrProvision(id, email, name).role().name());
    }

    @Transactional
    public UserDto updateProfile(UUID id, UpdateUserRequest request) {
        return UserApiMapper.toDto(userUseCases.updateProfile(id, request.name()));
    }

    // --- Administración -------------------------------------------------------------------

    /** Búsqueda por email o nombre; con {@code q} vacío lista todos. */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(String q, Pageable pageable) {
        List<SortOrder> sort = pageable.getSort().stream()
            .map(order -> new SortOrder(order.getProperty(), order.isAscending())).toList();
        UserPage users = userUseCases.listUsers(q, pageable.getPageNumber(), pageable.getPageSize(), sort);
        return new PageResponse<>(users.content().stream().map(UserApiMapper::toDto).toList(), users.page(), users.size(),
            users.totalElements(), users.totalPages());
    }

    /** Un ADMIN no puede quitarse su propio rol: evita dejar el sistema sin administradores por error. */
    @Transactional
    public UserDto changeRole(UUID adminId, UUID targetId, Role role) {
        return UserApiMapper.toDto(userUseCases.changeRole(adminId, targetId, UserRole.valueOf(role.name())));
    }
}
