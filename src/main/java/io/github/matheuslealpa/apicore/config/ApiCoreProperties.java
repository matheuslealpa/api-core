package io.github.matheuslealpa.apicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * api-core properties. Each feature can be turned off with {@code api-core.<feature>.enabled=false}.
 */
@ConfigurationProperties("api-core")
public record ApiCoreProperties(
        @DefaultValue Banner banner,
        @DefaultValue Defaults defaults,
        @DefaultValue Cache cache,
        @DefaultValue Audit audit,
        @DefaultValue Security security,
        @DefaultValue Web web,
        @DefaultValue Rsql rsql) {

    /**
     * api-core banner printed below the Spring Boot banner.
     */
    public record Banner(@DefaultValue("true") boolean enabled) {
    }

    /**
     * Opinionated defaults (actuator, tracing, virtual threads...) applied with the lowest precedence.
     */
    public record Defaults(@DefaultValue("true") boolean enabled) {
    }

    /**
     * Caffeine cache.
     *
     * @param defaultSpec Caffeine spec used by caches without a specific configuration
     * @param specs       specs by cache name, e.g. {@code api-core.cache.specs.products=maximumSize=500,expireAfterWrite=5m}
     */
    public record Cache(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("maximumSize=1000,expireAfterWrite=10m") String defaultSpec,
            Map<String, String> specs) {

        public Cache {
            specs = specs == null ? Map.of() : Map.copyOf(specs);
        }
    }

    /**
     * JPA auditing (createdBy, updatedBy, createdAt, updatedAt).
     *
     * @param systemAuditor  auditor used when there is no authenticated user
     * @param usernameClaims JWT claims checked, in order, to identify the user (fallback: {@code sub})
     */
    public record Audit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("system") String systemAuditor,
            @DefaultValue("preferred_username") List<String> usernameClaims) {
    }

    /**
     * Spring Security as an OAuth2 Resource Server (JWT).
     *
     * @param publicPaths    paths allowed without authentication
     * @param rolesClaim     claim holding the roles; accepts a nested path, e.g. {@code realm_access.roles} (Keycloak)
     * @param rolePrefix     prefix applied to the roles from the claim
     * @param methodSecurity enables {@code @PreAuthorize}/{@code @PostAuthorize}
     */
    public record Security(
            @DefaultValue("true") boolean enabled,
            @DefaultValue({"/actuator/health/**", "/actuator/info", "/error"}) List<String> publicPaths,
            @DefaultValue("roles") String rolesClaim,
            @DefaultValue("ROLE_") String rolePrefix,
            @DefaultValue("true") boolean methodSecurity,
            @DefaultValue Cors cors) {
    }

    /**
     * CORS. Only registered when {@code allowed-origins} is set.
     */
    public record Cors(
            List<String> allowedOrigins,
            @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
            @DefaultValue("*") List<String> allowedHeaders,
            List<String> exposedHeaders,
            @DefaultValue("false") boolean allowCredentials,
            @DefaultValue("1h") Duration maxAge) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
            exposedHeaders = exposedHeaders == null ? List.of() : List.copyOf(exposedHeaders);
        }
    }

    /**
     * Global exception handling with {@code ProblemDetail}.
     */
    public record Web(@DefaultValue("true") boolean enabled) {
    }

    /**
     * RSQL filters on {@code Specification} parameters annotated with {@code @RsqlFilter}.
     *
     * @param paramName default name of the request parameter holding the expression, e.g. {@code ?filter=name==abc}
     */
    public record Rsql(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("filter") String paramName) {
    }
}
