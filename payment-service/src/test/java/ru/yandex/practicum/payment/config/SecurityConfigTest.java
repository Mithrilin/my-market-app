package ru.yandex.practicum.payment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final NestedResourceAccessRolesConverter converter = new NestedResourceAccessRolesConverter();

    private Jwt jwtWithResourceAccess(Map<String, Object> resourceAccess) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", resourceAccess)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    @DisplayName("Converts nested resource_access.market-app.roles to authorities")
    void convert_shouldExtractServiceRole() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
                "market-app", Map.of("roles", List.of("SERVICE"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("SERVICE");
    }

    @Test
    @DisplayName("Returns empty when resource_access is missing")
    void convert_shouldReturnEmptyWhenResourceAccessMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when market-app block is missing")
    void convert_shouldReturnEmptyWhenClientMissing() {
        Jwt jwt = jwtWithResourceAccess(Map.of("other-client", Map.of("roles", List.of("SERVICE"))));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty when roles are absent")
    void convert_shouldReturnEmptyWhenRolesMissing() {
        Jwt jwt = jwtWithResourceAccess(Map.of("market-app", Map.of()));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("Ignores non-string roles and extracts the string ones")
    void convert_shouldIgnoreNonStringRoles() {
        Jwt jwt = jwtWithResourceAccess(Map.of(
                "market-app", Map.of("roles", List.of("SERVICE", 42))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SERVICE");
    }
}