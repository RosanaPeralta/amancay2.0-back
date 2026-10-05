package com.amancay.infrastructure.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Solo para desarrollo local: autentica cada request como un usuario fijo, sin token. */
public class DevUserAuthenticationFilter extends OncePerRequestFilter {

    private final LoggedUser devUser;

    public DevUserAuthenticationFilter(UUID id, String email) {
        this.devUser = new LoggedUser(id, email, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(new LoggedUserAuthenticationToken(devUser, null, List.of()));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
