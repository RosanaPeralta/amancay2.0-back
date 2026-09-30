package com.amancay.controllers;

public class UserReviewController extends com.amancay.infrastructure.adapters.in.web.UserReviewController {
    public UserReviewController(com.amancay.service.ReviewService reviewService) {
        super(reviewService);
    }
}
