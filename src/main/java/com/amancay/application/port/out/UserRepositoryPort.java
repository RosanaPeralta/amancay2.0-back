package com.amancay.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.User;

public interface UserRepositoryPort {
    Optional<User> findById(UUID id);

    List<User> findAllById(Collection<UUID> ids);

    // Alta como BUYER si no existe. Dos primeros requests simultaneos no rompen la
    // transaccion: el segundo no inserta nada y despues lee al que gano.
    void insertIfAbsent(UUID id, String email, String name);

    // Bloquea la fila del usuario hasta el commit (serializa cambios sobre sus direcciones).
    Optional<User> findByIdForUpdate(UUID id);

    // Mas nuevos primero.
    PageResult<User> findAll(PageQuery page);

    // Por email o nombre, sin distinguir mayusculas. Mas nuevos primero.
    PageResult<User> search(String text, PageQuery page);

    User save(User user);
}
