package io.github.matheuslealpa.apicore.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Adds authorization rules before the default {@code anyRequest().authenticated()}.
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
