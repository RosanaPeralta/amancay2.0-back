package com.amancay.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.amancay.security.DevUserAuthenticationFilter;
import com.amancay.security.SupabaseJwtAuthenticationConverter;
import com.amancay.security.UserRoleAuthoritiesFilter;
import com.amancay.service.UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SupabaseJwtAuthenticationConverter jwtAuthenticationConverter;

    private final UserService userService;

    @Value("${amancay.security.dev-user.enabled:false}")
    private boolean devUserEnabled;

    @Value("${amancay.security.dev-user.id:00000000-0000-0000-0000-000000000001}")
    private String devUserId;

    @Value("${amancay.security.dev-user.email:dev@amancay.local}")
    private String devUserEmail;

    public SecurityConfig(SupabaseJwtAuthenticationConverter jwtAuthenticationConverter, UserService userService) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userService = userService;
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${supabase.jwt.jwks-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (devUserEnabled) {
            log.warn("DEVELOPMENT AUTHENTICATION BYPASS IS ENABLED: every request runs as {} <{}>. "
                    + "Never enable amancay.security.dev-user.enabled outside local development.",
                    devUserId, devUserEmail);
            http.addFilterBefore(new DevUserAuthenticationFilter(UUID.fromString(devUserId), devUserEmail),
                    BearerTokenAuthenticationFilter.class);
        }

        // Runs after both authentication filters: the dev-user filter is registered before the
        // bearer-token filter, so anything after the latter sees whichever one authenticated.
        http.addFilterAfter(new UserRoleAuthoritiesFilter(userService), BearerTokenAuthenticationFilter.class);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO: reemplazar por las reglas de autorizacion definitivas (que endpoints
                // requieren auth, que roles, etc). Por ahora queda todo publico, salvo lo que
                // necesita si o si un usuario autenticado: el perfil y sus recursos (/api/me/**)
                // y el panel de administracion (/api/admin/**, que ademas exige ROLE_ADMIN via
                // @PreAuthorize en cada controller).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me/**", "/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
