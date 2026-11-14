package io.github.matheuslealpa.apicore.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.github.matheuslealpa.apicore.security.ClaimRolesConverter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * MockMvc helpers to authenticate with a JWT carrying roles, mapped the same way api-core maps them at runtime
 * ({@code ROLE_} prefix, nested claims such as Keycloak's {@code realm_access.roles}).
 *
 * <pre>{@code
 * mvc.perform(delete("/products/{id}", id).with(ApiCoreJwt.jwtWithRoles("ADMIN")))
 *         .andExpect(status().isNoContent());
 * }</pre>
 *
 * Shipped in the {@code tests} classifier of api-core; it needs {@code spring-security-test} on the test classpath.
 */
public final class ApiCoreJwt {

    private static final String DEFAULT_ROLES_CLAIM = "roles";

    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    private ApiCoreJwt() {
    }

    /**
     * JWT with the roles in the default {@code roles} claim ({@code api-core.security.roles-claim}).
     */
    public static JwtRequestPostProcessor jwtWithRoles(String... roles) {
        return jwtWithClaimRoles(DEFAULT_ROLES_CLAIM, roles);
    }

    /**
     * JWT with the roles in the given claim, which may be a dotted path such as {@code realm_access.roles}.
     * Use it when the application sets {@code api-core.security.roles-claim}.
     */
    public static JwtRequestPostProcessor jwtWithClaimRoles(String rolesClaim, String... roles) {
        String[] path = rolesClaim.split("\\.");
        Object value = List.of(roles);
        for (int i = path.length - 1; i > 0; i--) {
            value = Map.of(path[i], value);
        }
        String topLevelClaim = path[0];
        Object claimValue = value;
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user")
                .claim(topLevelClaim, claimValue).build();
        // Authorities are computed eagerly, so a later .jwt(...) customization does not drop them.
        Collection<GrantedAuthority> authorities = new ClaimRolesConverter(rolesClaim, DEFAULT_ROLE_PREFIX).convert(jwt);
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder.claim(topLevelClaim, claimValue))
                .authorities(authorities);
    }

}
