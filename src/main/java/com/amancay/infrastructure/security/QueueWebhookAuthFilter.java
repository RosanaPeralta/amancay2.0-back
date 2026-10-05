package com.amancay.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege {@code /internal/events/**} con una clave compartida simple: quien llama es el
 * futuro servicio de cola, no un usuario con JWT de Supabase. Placeholder mientras no se
 * defina con ese servicio un mecanismo mas robusto (HMAC de la entrega, mTLS, etc.) — fail
 * closed: sin {@code amancay.events.webhook.api-key} configurada, rechaza todo en vez de
 * dejar el endpoint abierto.
 */
public class QueueWebhookAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String PROTECTED_PATH_PREFIX = "/internal/";

    private final String expectedApiKey;

    public QueueWebhookAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(PROTECTED_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(HEADER);
        if (expectedApiKey == null || expectedApiKey.isBlank() || !expectedApiKey.equals(providedApiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
