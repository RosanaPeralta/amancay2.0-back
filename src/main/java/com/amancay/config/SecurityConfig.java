package com.amancay.config;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

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

    @Value("${amancay.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    public SecurityConfig(SupabaseJwtAuthenticationConverter jwtAuthenticationConverter, UserService userService) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userService = userService;
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${supabase.jwt.jwks-uri}") String jwkSetUri,
            @Value("${supabase.jwt.issuer}") String issuer) {
        // Supabase firma con ES256 (JWT Signing Keys); RS256 queda por si rotan a RSA.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithms(algorithms -> {
                    algorithms.add(SignatureAlgorithm.ES256);
                    algorithms.add(SignatureAlgorithm.RS256);
                })
                .build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains("authenticated"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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

        // Corre despues de los dos filtros de autenticacion: el del dev-user se registra antes
        // del de bearer-token, asi que lo que va despues de este ultimo ve al que haya autenticado.
        http.addFilterAfter(new UserRoleAuthoritiesFilter(userService), BearerTokenAuthenticationFilter.class);

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Requieren usuario autenticado: el perfil y sus recursos (/api/me/**), el panel de
                // administracion (/api/admin/**, que ademas exige ROLE_ADMIN via @PreAuthorize en
                // cada controller) y la escritura de resenias. El resto queda publico.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/me/**", "/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpStatus.FORBIDDEN, "Access denied")));

        return http.build();
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
