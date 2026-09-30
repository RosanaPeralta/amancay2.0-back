package com.amancay.controllers;

public class UserController extends com.amancay.infrastructure.adapters.in.web.UserController {
    public UserController(com.amancay.service.UserService userService) {
        super(userService);
    }
}
