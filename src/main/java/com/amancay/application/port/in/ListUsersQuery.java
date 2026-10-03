package com.amancay.application.port.in;

import java.util.UUID;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.User;

public interface ListUsersQuery {
    // Busqueda por email o nombre; con text vacio lista todos.
    PageResult<User> list(UUID requesterId, String text, PageQuery page);
}
