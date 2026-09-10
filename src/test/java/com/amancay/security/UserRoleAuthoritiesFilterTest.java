package com.amancay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.amancay.entity.Role;
import com.amancay.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserRoleAuthoritiesFilterTest {

    @Mock
    private UserService userService;

    private UserRoleAuthoritiesFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserRoleAuthoritiesFilter(userService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsRoleAdminToAnAdminUser() throws Exception {
        LoggedUser loggedUser = authenticate("admin@amancay.com");
        when(userService.getOrProvisionRole(loggedUser.id(), loggedUser.email())).thenReturn(Role.ADMIN);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(authorities()).containsExactly("ROLE_ADMIN");
        assertThat(currentAuthentication().getPrincipal()).isEqualTo(loggedUser);
        assertThat(currentAuthentication().isAuthenticated()).isTrue();
    }

    @Test
    void grantsRoleBuyerToABuyerUser() throws Exception {
        LoggedUser loggedUser = authenticate("buyer@amancay.com");
        when(userService.getOrProvisionRole(loggedUser.id(), loggedUser.email())).thenReturn(Role.BUYER);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(authorities()).containsExactly("ROLE_BUYER");
        assertThat(currentAuthentication().getPrincipal()).isEqualTo(loggedUser);
    }

    @Test
    void leavesAnUnauthenticatedRequestUntouchedWithoutReadingTheDatabase() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(userService);
    }

    private LoggedUser authenticate(String email) {
        LoggedUser loggedUser = new LoggedUser(UUID.randomUUID(), email);
        SecurityContextHolder.getContext()
                .setAuthentication(new LoggedUserAuthenticationToken(loggedUser, null, List.of()));
        return loggedUser;
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private List<String> authorities() {
        return currentAuthentication().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
