package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name,
            Pageable pageable);
}
