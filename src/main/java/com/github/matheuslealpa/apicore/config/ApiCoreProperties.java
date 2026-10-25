package com.github.matheuslealpa.apicore.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriedades do api-core. Cada feature pode ser desligada com {@code api-core.<feature>.enabled=false}.
 */
@ConfigurationProperties("api-core")
public record ApiCoreProperties(
        @DefaultValue Defaults defaults,
        @DefaultValue Cache cache,
        @DefaultValue Audit audit,
        @DefaultValue Security security,
        @DefaultValue Web web) {

    /**
     * Defaults opinativos (actuator, tracing, virtual threads...) aplicados com a menor precedência.
     */
    public record Defaults(@DefaultValue("true") boolean enabled) {
    }

    /**
     * Cache Caffeine.
     *
     * @param defaultSpec spec Caffeine usada por caches sem configuração específica
     * @param specs specs por nome de cache, ex.: {@code api-core.cache.specs.produtos=maximumSize=500,expireAfterWrite=5m}
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
     * Auditoria JPA (createdBy, updatedBy, createdAt, updatedAt).
     *
     * @param systemAuditor auditor usado quando não há usuário autenticado
     * @param usernameClaims claims do JWT consultadas, em ordem, para identificar o usuário (fallback: {@code sub})
     */
    public record Audit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("system") String systemAuditor,
            @DefaultValue("preferred_username") List<String> usernameClaims) {
    }

    /**
     * Spring Security como OAuth2 Resource Server (JWT).
     *
     * @param publicPaths rotas liberadas sem autenticação
     * @param rolesClaim claim com as roles; aceita caminho aninhado, ex.: {@code realm_access.roles} (Keycloak)
     * @param rolePrefix prefixo aplicado às roles do claim
     * @param methodSecurity habilita {@code @PreAuthorize}/{@code @PostAuthorize}
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
     * CORS. Só é registrado quando {@code allowed-origins} é informado.
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
     * Tratamento global de exceções com {@code ProblemDetail}.
     */
    public record Web(@DefaultValue("true") boolean enabled) {
    }
}
