package com.amancay.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        LoggedUser loggedUser = new LoggedUser(id, email, nameFrom(jwt));
        return new LoggedUserAuthenticationToken(loggedUser, jwt, List.of());
    }

    private static String nameFrom(Jwt jwt) {
        Map<String, Object> metadata = jwt.getClaimAsMap("user_metadata");
        if (metadata == null) {
            return null;
        }
        Object name = metadata.getOrDefault("name", metadata.get("full_name"));
        return name instanceof String text && !text.isBlank() ? text : null;
    }
}
