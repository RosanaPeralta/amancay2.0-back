package com.amancay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class SupabaseJwtAuthenticationConverterTest {

    private final SupabaseJwtAuthenticationConverter converter = new SupabaseJwtAuthenticationConverter();

    @Test
    void buildsTheLoggedUserFromSubjectEmailAndMetadataName() {
        UUID id = UUID.randomUUID();
        Jwt jwt = jwt(id).claim("user_metadata", Map.of("name", "Ada")).build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isEqualTo(new LoggedUser(id, "ada@amancay.com", "Ada"));
        assertThat(authentication.getCredentials()).isSameAs(jwt);
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void fallsBackToFullNameWhenNameIsMissing() {
        Jwt jwt = jwt(UUID.randomUUID()).claim("user_metadata", Map.of("full_name", "Ada Lovelace")).build();

        LoggedUser loggedUser = (LoggedUser) converter.convert(jwt).getPrincipal();

        assertThat(loggedUser.name()).isEqualTo("Ada Lovelace");
    }

    @Test
    void leavesNameNullWithoutMetadata() {
        Jwt jwt = jwt(UUID.randomUUID()).build();

        LoggedUser loggedUser = (LoggedUser) converter.convert(jwt).getPrincipal();

        assertThat(loggedUser.name()).isNull();
    }

    @Test
    void leavesNameNullWhenMetadataNameIsBlank() {
        Jwt jwt = jwt(UUID.randomUUID()).claim("user_metadata", Map.of("name", "   ")).build();

        LoggedUser loggedUser = (LoggedUser) converter.convert(jwt).getPrincipal();

        assertThat(loggedUser.name()).isNull();
    }

    @Test
    void rejectsANonUuidSubject() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "ES256").subject("not-a-uuid").build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Jwt.Builder jwt(UUID id) {
        return Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .subject(id.toString())
                .claim("email", "ada@amancay.com");
    }
}
