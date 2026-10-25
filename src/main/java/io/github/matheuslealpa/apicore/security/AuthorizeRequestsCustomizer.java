package io.github.matheuslealpa.apicore.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Permite adicionar regras de autorização antes do {@code anyRequest().authenticated()} padrão.
 *
 * <pre>{@code
 * @Bean
 * AuthorizeRequestsCustomizer adminRules() {
 *     return auth -> auth.requestMatchers("/admin/**").hasRole("ADMIN");
 * }
 * }</pre>
 */
@FunctionalInterface
public interface AuthorizeRequestsCustomizer
        extends Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> {
}
