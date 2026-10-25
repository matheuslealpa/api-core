package io.github.matheuslealpa.apicore.security;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;

/**
 * API stateless protegida por JWT (OAuth2 Resource Server). O {@code JwtDecoder} vem das properties
 * nativas {@code spring.security.oauth2.resourceserver.jwt.*}. Um {@link SecurityFilterChain} próprio
 * da aplicação substitui o padrão.
 */
@AutoConfiguration(before = {ManagementWebSecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, Jwt.class})
@ConditionalOnBooleanProperty(name = "api-core.security.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter(ApiCoreProperties properties) {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        ClaimRolesConverter roles = new ClaimRolesConverter(
                properties.security().rolesClaim(), properties.security().rolePrefix());
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            authorities.addAll(roles.convert(jwt));
            return authorities;
        });
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailSecurityHandler problemDetailSecurityHandler(ObjectProvider<JsonMapper> jsonMapper) {
        return new ProblemDetailSecurityHandler(jsonMapper.getIfAvailable(JsonMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain apiCoreSecurityFilterChain(HttpSecurity http, ApiCoreProperties properties,
                                                          JwtAuthenticationConverter jwtAuthenticationConverter, ProblemDetailSecurityHandler problemHandler,
                                                          ObjectProvider<AuthorizeRequestsCustomizer> authorizeCustomizers) throws Exception {
        String[] publicPaths = properties.security().publicPaths().toArray(String[]::new);
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(publicPaths).permitAll();
                    authorizeCustomizers.orderedStream().forEach(customizer -> customizer.customize(auth));
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));
        return http.build();
    }

    /**
     * Sem {@code api-core.security.cors.allowed-origins} nenhuma origem externa é liberada.
     */
    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource(ApiCoreProperties properties) {
        ApiCoreProperties.Cors cors = properties.security().cors();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!cors.allowedOrigins().isEmpty()) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOriginPatterns(cors.allowedOrigins());
            configuration.setAllowedMethods(cors.allowedMethods());
            configuration.setAllowedHeaders(cors.allowedHeaders());
            configuration.setExposedHeaders(cors.exposedHeaders());
            configuration.setAllowCredentials(cors.allowCredentials());
            configuration.setMaxAge(cors.maxAge());
            source.registerCorsConfiguration("/**", configuration);
        }
        return source;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty(name = "api-core.security.method-security", matchIfMissing = true)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
