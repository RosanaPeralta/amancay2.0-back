package com.amancay.infrastructure.adapter.out.persistence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.amancay.application.port.out.BuyerEmailPort;
import com.amancay.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.amancay.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

@Component
class BuyerEmailAdapter implements BuyerEmailPort {

    private final SpringDataUserRepository userRepository;

    BuyerEmailAdapter(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<UUID, String> findEmailsByUserId(Set<UUID> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getEmail));
    }
}
