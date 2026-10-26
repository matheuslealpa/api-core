package io.github.matheuslealpa.apicore.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extracts roles from a JWT claim. Accepts a dotted nested path, e.g. {@code realm_access.roles}.
 */
public class ClaimRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String[] claimPath;

    private final String prefix;

    public ClaimRolesConverter(String claim, String prefix) {
        this.claimPath = claim.split("\\.");
        this.prefix = prefix;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object value = jwt.getClaims();
        for (String key : claimPath) {
            if (!(value instanceof Map<?, ?> map)) {
                return List.of();
            }
            value = map.get(key);
        }
        return switch (value) {
            case Collection<?> roles -> roles.stream().map(role -> authority(role.toString())).toList();
            case String roles -> List.of(roles.split("[\\s,]+")).stream()
                    .filter(role -> !role.isBlank())
                    .map(this::authority)
                    .toList();
            case null, default -> List.of();
        };
    }

    private GrantedAuthority authority(String role) {
        return new SimpleGrantedAuthority(role.startsWith(prefix) ? role : prefix + role);
    }
}
