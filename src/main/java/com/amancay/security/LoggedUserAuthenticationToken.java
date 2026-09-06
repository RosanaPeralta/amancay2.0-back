package com.amancay.security;

import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class LoggedUserAuthenticationToken extends AbstractAuthenticationToken {

    private final LoggedUser principal;
    private final Jwt jwt;

    public LoggedUserAuthenticationToken(LoggedUser principal, Jwt jwt) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.principal = principal;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
