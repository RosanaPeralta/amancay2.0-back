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

/**
 * Protege el bypass de autenticación de desarrollo.
 *
 * <p>Qué cubre:
 * <ul>
 *   <li>Que el filtro autentique al usuario fijo configurado cuando no hay ninguna autenticación
 *       previa en el contexto. Es lo que permite ejercitar los endpoints protegidos sin token
 *       mientras la integración con Supabase sigue bloqueada.</li>
 *   <li>Que <strong>no pise</strong> una autenticacion existente. Este es el caso importante: si el
 *       filtro sobrescribiera un token real, cualquier request autenticado de verdad pasaría a
 *       ejecutarse con la identidad del usuario de desarrollo. Sería una suplantación silenciosa,
 *       sin error ni log.</li>
 * </ul>
 *
 * <p>Cuando se puede eliminar: junto con {@link DevUserAuthenticationFilter}. El filtro existe solo
 * porque hoy no se puede autenticar contra Supabase. El día que la autenticación real funcione y se
 * borre el filtro, este test se va con el. Mientras el filtro siga en el codigo, el test se queda:
 * es lo único que garantiza que el bypass no se convierta en un agujero de seguridad.
 *
 * <p>No usa contexto de Spring: instancia el filtro a mano y corre en milisegundos.
 */
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
