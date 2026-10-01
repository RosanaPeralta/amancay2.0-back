package com.amancay.application.port.in;

import com.amancay.domain.model.PageQuery;
import com.amancay.domain.model.PageResult;
import com.amancay.domain.model.User;

public interface ListUsersQuery {
    // Busqueda por email o nombre; con text vacio lista todos.
    PageResult<User> list(String text, PageQuery page);
}
