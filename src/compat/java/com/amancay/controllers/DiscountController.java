package com.amancay.controllers;

public class DiscountController extends com.amancay.infrastructure.adapters.in.web.DiscountController {
    public DiscountController(com.amancay.service.DiscountService discountService) {
        super(discountService);
    }
}
