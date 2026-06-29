package com.example.workflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void mapsKeycloakRealmRolesToExistingAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-id")
                .claim("preferred_username", "manager-local")
                .claim("realm_access", Map.of("roles", List.of("manager", "USER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) new SecurityConfig()
                .jwtAuthenticationConverter()
                .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("manager-local");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("MANAGER", "USER");
    }

    @Test
    void jwtWithoutRealmRolesHasNoAuthorities() {
        Jwt jwt = jwtBuilder()
                .claim("preferred_username", "customer")
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) new SecurityConfig()
                .jwtAuthenticationConverter()
                .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void jwtRoleConverterIgnoresBlankAndNonStringRoles() {
        Jwt jwt = jwtBuilder()
                .claim("preferred_username", "staff")
                .claim("realm_access", Map.of("roles", List.of("", "staff", 123)))
                .build();

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) new SecurityConfig()
                .jwtAuthenticationConverter()
                .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("STAFF");
    }

    @Test
    void corsConfigurationParsesConfiguredOrigins() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOrigins",
                "http://localhost:4200, https://app.example.com "
        );

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/products"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .containsExactly("http://localhost:4200", "https://app.example.com");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }
}
