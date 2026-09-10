package com.amancay.security;

import java.util.List;
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
        LoggedUser loggedUser = new LoggedUser(id, email);
        // The role is not in the token: UserRoleAuthoritiesFilter resolves it from the database
        // later in the chain.
        return new LoggedUserAuthenticationToken(loggedUser, jwt, List.of());
    }
}
