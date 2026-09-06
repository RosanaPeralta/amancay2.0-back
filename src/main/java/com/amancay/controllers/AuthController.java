package com.amancay.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amancay.security.LoggedUser;

@RestController
@RequestMapping("/api/me")
public class AuthController {

    @GetMapping
    public LoggedUser me(@AuthenticationPrincipal LoggedUser loggedUser) {
        return loggedUser;
    }
}
