package com.amancay.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.amancay.entity.Role;
import com.amancay.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Adds the role stored in {@code users.role} to the current authentication as a
 * {@code ROLE_BUYER} / {@code ROLE_ADMIN} authority, so any endpoint can be guarded with
 * {@code @PreAuthorize("hasRole('ADMIN')")} without touching security code.
 *
 * <p>The filter must be registered after authentication has happened in the chain: it reads the
 * {@link LoggedUser} left by the bearer-token filter (or by {@link DevUserAuthenticationFilter})
 * and replaces the authentication with an equivalent one that carries the role. Requests that are
 * not authenticated are left untouched, so public endpoints never hit the database.
 *
 * <p><strong>Cost:</strong> this performs one database read per authenticated request. It is the
 * simplest correct implementation and it keeps the role authoritative on every call; the obvious
 * optimisation, once this becomes measurable, is a short-lived cache of user id to role.
 */
public class UserRoleAuthoritiesFilter extends OncePerRequestFilter {

    private final UserService userService;

    public UserRoleAuthoritiesFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof LoggedUser loggedUser
                && authentication.getAuthorities().isEmpty()) {

            Role role = userService.getOrProvisionRole(loggedUser.id(), loggedUser.email());
            Jwt jwt = authentication.getCredentials() instanceof Jwt credentials ? credentials : null;

            SecurityContextHolder.getContext().setAuthentication(new LoggedUserAuthenticationToken(loggedUser, jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Also run on the ERROR dispatch. Without this, a request that fails validation is forwarded to
     * {@code /error} with an authentication that carries no role and comes back as 401 instead of
     * the status the handler produced.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
