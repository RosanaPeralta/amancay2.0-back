package com.amancay.order.adapter.out.user;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.amancay.entity.Role;
import com.amancay.exceptions.UserNotFoundException;
import com.amancay.order.application.port.out.LoadRequesterPort;
import com.amancay.repository.UserRepository;

@Component
class RequesterAdapter implements LoadRequesterPort {

    private final UserRepository userRepository;

    RequesterAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Requester load(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new Requester(user.getId(), user.getRole() == Role.ADMIN))
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
