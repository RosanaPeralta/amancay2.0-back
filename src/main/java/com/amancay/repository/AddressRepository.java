package com.amancay.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.amancay.entity.Address;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdOrderByCreatedAtAsc(UUID userId);

    long countByUserId(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Desmarca la predeterminada actual antes de marcar otra. Se ejecuta como UPDATE directo para
     * que el índice único parcial de la base ({@code uq_addresses_default_per_user}) nunca vea dos
     * en {@code true} a la vez; por eso limpia el contexto de persistencia y hay que recargar la
     * entidad después de llamarlo.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.userId = :userId AND a.defaultAddress = true")
    void clearDefaultByUserId(@Param("userId") UUID userId);
}
