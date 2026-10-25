package io.github.matheuslealpa.apicore.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditorAwareTests {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware("system", List.of("preferred_username"));

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semAutenticacaoUsaAuditorDeSistema() {
        assertThat(auditorAware.getCurrentAuditor()).hasValue("system");
    }

    @Test
    void usaClaimConfigurada() {
        authenticate(Map.of("sub", "123", "preferred_username", "maria"));
        assertThat(auditorAware.getCurrentAuditor()).hasValue("maria");
    }

    @Test
    void semClaimConfiguradaUsaSubject() {
        authenticate(Map.of("sub", "123"));
        assertThat(auditorAware.getCurrentAuditor()).hasValue("123");
    }

    private static void authenticate(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
