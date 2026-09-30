package com.amancay.controllers;

public class AdminUserController extends com.amancay.infrastructure.adapters.in.web.AdminUserController {
    public AdminUserController(com.amancay.service.UserService userService) {
        super(userService);
    }
}
