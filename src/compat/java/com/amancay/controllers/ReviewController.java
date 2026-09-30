package com.amancay.controllers;

public class ReviewController extends com.amancay.infrastructure.adapters.in.web.ReviewController {
    public ReviewController(com.amancay.service.ReviewService reviewService) {
        super(reviewService);
    }
}
