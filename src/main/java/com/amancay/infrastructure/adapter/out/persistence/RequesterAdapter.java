package com.amancay.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.LoadRequesterPort;
import com.amancay.domain.exception.UserNotFoundException;
import com.amancay.domain.model.Role;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

@Component
class RequesterAdapter implements LoadRequesterPort {

    private final SpringDataUserRepository userRepository;

    RequesterAdapter(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Requester load(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new Requester(user.getId(), user.getRole() == Role.ADMIN))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
