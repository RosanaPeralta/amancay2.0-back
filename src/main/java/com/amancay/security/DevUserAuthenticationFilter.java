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
 * Autentica cada solicitud como un usuario local fijo, de modo que los endpoints autenticados
 * puedan probarse sin un token de Supabase durante el desarrollo.
 *
 * <p>Este filtro solo se registra cuando {@code amancay.security.dev-user.enabled} es {@code true}.
 * Está deshabilitado de forma predeterminada y nunca debe habilitarse fuera del desarrollo local: mientras
 * esté activo, cada solicitante es tratado como el usuario configurado y no se requiere ningún token.
 *
 * <p>Una solicitud que ya incluye una autenticación se deja intacta, por lo que un token real sigue
 * teniendo prioridad cuando está presente.
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

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
