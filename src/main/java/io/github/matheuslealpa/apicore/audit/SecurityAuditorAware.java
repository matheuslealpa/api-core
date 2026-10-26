package io.github.matheuslealpa.apicore.audit;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves the auditor from the user authenticated in Spring Security.
 */
public class SecurityAuditorAware implements AuditorAware<String> {

    private final String systemAuditor;

    private final List<String> usernameClaims;

    public SecurityAuditorAware(String systemAuditor, List<String> usernameClaims) {
        this.systemAuditor = systemAuditor;
        this.usernameClaims = List.copyOf(usernameClaims);
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(systemAuditor);
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            for (String claim : usernameClaims) {
                String value = jwt.getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }
        return Optional.of(authentication.getName());
    }
}
