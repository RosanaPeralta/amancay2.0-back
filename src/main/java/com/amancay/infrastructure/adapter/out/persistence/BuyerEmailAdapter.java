package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.entity.User;
import com.amancay.repository.UserRepository;

@Component
class BuyerEmailAdapter implements BuyerEmailPort {

    private final UserRepository userRepository;

    BuyerEmailAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<UUID, String> findEmailsByUserId(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }
}
