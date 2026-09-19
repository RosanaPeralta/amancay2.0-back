package com.amancay.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.amancay.config.SecurityConfig;
import com.amancay.controllers.AdminUserController;
import com.amancay.controllers.ProductReviewController;
import com.amancay.controllers.UserController;
import com.amancay.dto.PageResponse;
import com.amancay.dto.UserDto;
import com.amancay.entity.Role;
import com.amancay.service.ReviewService;
import com.amancay.service.UserService;

/**
 * Verifica las reglas de autorizacion de {@link SecurityConfig} de punta a punta con MockMvc:
 * que endpoints exigen token, que rol necesita el panel admin, el formato JSON de 401/403 y CORS.
 */
@WebMvcTest(controllers = {UserController.class, AdminUserController.class, ProductReviewController.class},
        properties = {
                "supabase.jwt.issuer=https://test.supabase.co/auth/v1",
                "supabase.jwt.jwks-uri=https://test.supabase.co/auth/v1/.well-known/jwks.json"})
@Import({SecurityConfig.class, SupabaseJwtAuthenticationConverter.class})
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void meWithoutTokenIsUnauthorizedWithJsonBody() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void meWithABuyerTokenIsOk() throws Exception {
        LoggedUser buyer = loggedUser("buyer@amancay.com");
        when(userService.getOrProvisionRole(buyer.id(), buyer.email(), buyer.name())).thenReturn(Role.BUYER);
        when(userService.getOrProvision(buyer.id(), buyer.email(), buyer.name()))
                .thenReturn(new UserDto(buyer.id(), buyer.email(), buyer.name(), Role.BUYER, Instant.now()));

        mockMvc.perform(get("/api/me").with(authentication(tokenFor(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("buyer@amancay.com"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void adminUsersAsBuyerIsForbiddenWithJsonBody() throws Exception {
        LoggedUser buyer = loggedUser("buyer@amancay.com");
        when(userService.getOrProvisionRole(buyer.id(), buyer.email(), buyer.name())).thenReturn(Role.BUYER);

        mockMvc.perform(get("/api/admin/users").with(authentication(tokenFor(buyer))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void adminUsersAsAdminIsOk() throws Exception {
        LoggedUser admin = loggedUser("admin@amancay.com");
        when(userService.getOrProvisionRole(admin.id(), admin.email(), admin.name())).thenReturn(Role.ADMIN);
        when(userService.listUsers(isNull(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/users").with(authentication(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void creatingAReviewWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/reviews", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"title\":\"Great\",\"comment\":\"Loved it\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void corsPreflightFromTheFrontIsAllowed() throws Exception {
        mockMvc.perform(options("/api/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization"));
    }

    private static LoggedUser loggedUser(String email) {
        return new LoggedUser(UUID.randomUUID(), email, "Ada");
    }

    private static Authentication tokenFor(LoggedUser loggedUser) {
        return new LoggedUserAuthenticationToken(loggedUser, null, List.of());
    }
}
