package com.amancay.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class DevUserAuthenticationFilterTest {

    private static final UUID DEV_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEV_EMAIL = "dev@amancay.local";

    private final DevUserAuthenticationFilter filter = new DevUserAuthenticationFilter(DEV_ID, DEV_EMAIL);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesTheConfiguredDevUserWhenTheContextIsEmpty() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new LoggedUser(DEV_ID, DEV_EMAIL));
    }

    @Test
    void leavesAnExistingAuthenticationUntouched() throws Exception {
        LoggedUser realUser = new LoggedUser(UUID.randomUUID(), "real@amancay.com");
        SecurityContextHolder.getContext().setAuthentication(new LoggedUserAuthenticationToken(realUser, null, List.of()));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(realUser);
    }
}
