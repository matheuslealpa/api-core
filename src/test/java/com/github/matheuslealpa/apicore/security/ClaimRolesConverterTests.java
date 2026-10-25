package com.github.matheuslealpa.apicore.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimRolesConverterTests {

    @Test
    void lerRolesDeClaimSimples() {
        Jwt jwt = jwt(Map.of("roles", List.of("ADMIN", "ROLE_USER")));
        assertThat(authorities(new ClaimRolesConverter("roles", "ROLE_"), jwt))
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void lerRolesDeClaimAninhadoEstiloKeycloak() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("admin"))));
        assertThat(authorities(new ClaimRolesConverter("realm_access.roles", "ROLE_"), jwt))
                .containsExactly("ROLE_admin");
    }

    @Test
    void lerRolesSeparadasPorEspaco() {
        Jwt jwt = jwt(Map.of("roles", "a b"));
        assertThat(authorities(new ClaimRolesConverter("roles", "ROLE_"), jwt)).containsExactly("ROLE_a", "ROLE_b");
    }

    @Test
    void claimAusenteNaoGeraRoles() {
        assertThat(new ClaimRolesConverter("realm_access.roles", "ROLE_").convert(jwt(Map.of("x", "y")))).isEmpty();
    }

    private static List<String> authorities(ClaimRolesConverter converter, Jwt jwt) {
        return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), claims);
    }
}
