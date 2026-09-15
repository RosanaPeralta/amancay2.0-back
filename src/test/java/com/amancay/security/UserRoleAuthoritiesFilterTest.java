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

/**
 * Protege la resolución de roles que habilita las autorizaciones por rol de toda la API.
 *
 * <p>Qué cubre:
 * <ul>
 *   <li>Que el rol guardado en {@code users.role} se traduzca a las authorities {@code ROLE_ADMIN}
 *       y {@code ROLE_BUYER}. El prefijo {@code ROLE_} no es decorativo: es lo que hace que
 *       {@code hasRole('ADMIN')} matchee. Si alguien lo saca, las anotaciones de seguridad dejan de
 *       coincidir <strong>en silencio</strong> y los endpoints de administración quedan abiertos,
 *       sin ningun error visible.</li>
 *   <li>Que un request sin autenticar se deje intacto y <strong>no lea la base de datos</strong>.
 *       El filtro hace una consulta por request autenticado; si esa guarda se rompe, cada request
 *       público (por ejemplo el listado de productos o de reseñas) pasa a pegarle a Supabase.</li>
 * </ul>
 *
 * <p>Cuando se puede eliminar: cuando se elimine {@link UserRoleAuthoritiesFilter}, es decir si el
 * rol pasa a viajar dentro del token de Supabase como claim y deja de resolverse contra la base. En
 * ese escenario la lógica se mudaría al converter del token y los tests deberían mudarse con ella,
 * no borrarse. Mientras el rol se resuelva por consulta, este test se queda.
 *
 * <p>No levanta contexto de Spring: mockea {@link UserService} y corre en milisegundos.
 */
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
