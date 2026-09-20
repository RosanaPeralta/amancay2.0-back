package com.amancay.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.amancay.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    // PostgreSQL resolves simultaneous first requests without aborting the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO users (id, email, name, role, is_active, created_at, updated_at)
            VALUES (:id, :email, :name, 'BUYER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("email") String email, @Param("name") String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name,
            Pageable pageable);
}
