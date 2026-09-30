package com.amancay.controllers;

public class AdminReviewController extends com.amancay.infrastructure.adapters.in.web.AdminReviewController {
    public AdminReviewController(com.amancay.service.ReviewService reviewService) {
        super(reviewService);
    }
}
