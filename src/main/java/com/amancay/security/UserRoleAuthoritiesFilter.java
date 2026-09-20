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
import com.amancay.exceptions.InactiveUserException;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Agrega el rol almacenado en {@code users.role} a la autenticación actual como una
 * autoridad {@code ROLE_BUYER} / {@code ROLE_ADMIN}, de modo que cualquier endpoint pueda
 * protegerse con {@code @PreAuthorize("hasRole('ADMIN')")} sin modificar el código de seguridad.
 *
 * <p>El filtro debe registrarse después de que la autenticación haya ocurrido en la cadena: lee el
 * {@link LoggedUser} dejado por el filtro de token bearer (o por {@link DevUserAuthenticationFilter})
 * y reemplaza la autenticación por una equivalente que contenga el rol. Las solicitudes que no
 * están autenticadas se dejan intactas, por lo que los endpoints públicos nunca consultan la base de datos.
 */
public class UserRoleAuthoritiesFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(UserRoleAuthoritiesFilter.class);

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

            Role role;
            try {
                role = userService.getOrProvisionRole(loggedUser.id(), loggedUser.email(), loggedUser.name());
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
