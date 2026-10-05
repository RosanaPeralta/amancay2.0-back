package com.amancay.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.infrastructure.adapter.out.persistence.entity.UserJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    // PostgreSQL resolves simultaneous first requests without aborting the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO users (id, email, name, role, is_active, created_at, updated_at)
            VALUES (:id, :email, :name, 'BUYER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("email") String email, @Param("name") String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserJpaEntity u WHERE u.id = :id")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Page<UserJpaEntity> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name,
            Pageable pageable);
}
