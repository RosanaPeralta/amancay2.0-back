package com.amancay.application.port.in;

import java.util.List;

import com.amancay.domain.model.Category;

public interface ListCategoriesQuery {
    List<Category> listAll();
}
