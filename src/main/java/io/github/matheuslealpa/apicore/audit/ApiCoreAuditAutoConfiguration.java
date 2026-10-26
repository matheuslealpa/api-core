package io.github.matheuslealpa.apicore.audit;

import java.util.Optional;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;
import jakarta.persistence.EntityManager;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing. Backs off if the application already uses {@code @EnableJpaAuditing}.
 * An application-defined {@link AuditorAware} replaces the default.
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class, EnableJpaAuditing.class})
@ConditionalOnBooleanProperty(name = "api-core.audit.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreAuditAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(name = "jpaAuditingHandler")
    @EnableJpaAuditing
    static class JpaAuditingConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.Jwt")
    static class SecurityAuditorConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuditorAware.class)
        SecurityAuditorAware apiCoreAuditorAware(ApiCoreProperties properties) {
            return new SecurityAuditorAware(properties.audit().systemAuditor(), properties.audit().usernameClaims());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.jwt.Jwt")
    static class SystemAuditorConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuditorAware.class)
        AuditorAware<String> apiCoreAuditorAware(ApiCoreProperties properties) {
            return () -> Optional.of(properties.audit().systemAuditor());
        }
    }
}
