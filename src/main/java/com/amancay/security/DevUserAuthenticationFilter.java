package com.amancay.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates every request as a fixed local user, so the authenticated endpoints can be
 * exercised without a Supabase token during development.
 *
 * <p>This filter is only registered when {@code amancay.security.dev-user.enabled} is {@code true}.
 * It is disabled by default and must never be enabled outside local development: while active,
 * every caller is treated as the configured user and no token is required.
 *
 * <p>A request that already carries an authentication is left untouched, so a real token still
 * wins when one is present.
 */
public class DevUserAuthenticationFilter extends OncePerRequestFilter {

    private final LoggedUser devUser;

    public DevUserAuthenticationFilter(UUID id, String email) {
        this.devUser = new LoggedUser(id, email);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(new LoggedUserAuthenticationToken(devUser, null, List.of()));
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Also authenticate the ERROR dispatch. Without this, a request that fails validation is
     * forwarded to {@code /error} with an empty security context and comes back as 401 instead of
     * the 400 the handler produced.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
