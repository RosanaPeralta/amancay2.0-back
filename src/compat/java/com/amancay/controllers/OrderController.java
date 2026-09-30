package com.amancay.controllers;

public class OrderController extends com.amancay.infrastructure.adapters.in.web.OrderController {
    public OrderController(com.amancay.service.OrderService orderService) {
        super(orderService);
    }
}
