package com.amancay.controllers;

public class CategoryController extends com.amancay.infrastructure.adapters.in.web.CategoryController {
    public CategoryController(com.amancay.service.CategoryService categoryService) {
        super(categoryService);
    }
}
