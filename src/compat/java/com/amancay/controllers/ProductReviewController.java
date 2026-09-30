package com.amancay.controllers;

public class ProductReviewController extends com.amancay.infrastructure.adapters.in.web.ProductReviewController {
    public ProductReviewController(com.amancay.service.ReviewService reviewService) {
        super(reviewService);
    }
}
