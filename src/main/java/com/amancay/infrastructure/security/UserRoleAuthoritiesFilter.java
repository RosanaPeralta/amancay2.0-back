package com.amancay.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import com.amancay.application.port.in.GetOrProvisionUserUseCase;
import com.amancay.domain.exception.InactiveUserException;
import com.amancay.domain.model.Role;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Agrega el rol de {@code users.role} como authority {@code ROLE_*}. Los requests sin autenticar no consultan la base. */
public class UserRoleAuthoritiesFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(UserRoleAuthoritiesFilter.class);

    private final GetOrProvisionUserUseCase getOrProvisionUser;

    public UserRoleAuthoritiesFilter(GetOrProvisionUserUseCase getOrProvisionUser) {
        this.getOrProvisionUser = getOrProvisionUser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof LoggedUser loggedUser
                && authentication.getAuthorities().isEmpty()) {

            Role role;
            try {
                role = getOrProvisionUser.getOrProvision(loggedUser.id(), loggedUser.email(), loggedUser.name()).getRole();
            } catch (InactiveUserException exception) {
                reject(response, HttpServletResponse.SC_FORBIDDEN, "User is inactive");
                return;
            } catch (DataAccessException exception) {
                log.error("Unable to resolve authenticated user", exception);
                reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "User authentication unavailable");
                return;
            }
            Jwt jwt = authentication.getCredentials() instanceof Jwt credentials ? credentials : null;

            SecurityContextHolder.getContext().setAuthentication(new LoggedUserAuthenticationToken(loggedUser, jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        }

        filterChain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
