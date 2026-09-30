package com.amancay.controllers;

public class ProductController extends com.amancay.infrastructure.adapters.in.web.ProductController {
    public ProductController(com.amancay.service.ProductService productService,
            com.amancay.service.DiscountService discountService) {
        super(productService, discountService);
    }
}
