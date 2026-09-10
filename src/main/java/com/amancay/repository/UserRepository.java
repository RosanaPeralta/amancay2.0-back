package com.amancay.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}
