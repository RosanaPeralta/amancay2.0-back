package com.amancay.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class LoggedUserAuthenticationToken extends AbstractAuthenticationToken {

    private final LoggedUser principal;
    private final Jwt jwt;

    public LoggedUserAuthenticationToken(LoggedUser principal, Jwt jwt,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
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
