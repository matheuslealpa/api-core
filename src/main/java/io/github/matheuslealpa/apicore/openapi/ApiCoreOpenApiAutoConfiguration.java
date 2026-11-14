package io.github.matheuslealpa.apicore.openapi;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;
import io.github.matheuslealpa.apicore.rsql.ApiCoreRsqlAutoConfiguration;
import io.github.matheuslealpa.apicore.rsql.RsqlFilterArgumentResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;

/**
 * OpenAPI integration, active when springdoc is on the classpath: {@code Pageable} as {@code page}/{@code size}/{@code sort},
 * the RSQL {@code filter} parameter with its whitelist, and the JWT bearer security scheme.
 * Turn off with {@code api-core.openapi.enabled=false}.
 */
@AutoConfiguration(after = ApiCoreRsqlAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OperationCustomizer.class)
@ConditionalOnBooleanProperty(name = "api-core.openapi.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreOpenApiAutoConfiguration {

    static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    @ConditionalOnMissingBean
    PageableOperationCustomizer pageableOperationCustomizer(Environment environment) {
        return new PageableOperationCustomizer(
                environment.getProperty("spring.data.web.pageable.prefix", ""),
                environment.getProperty("spring.data.web.pageable.page-parameter", "page"),
                environment.getProperty("spring.data.web.pageable.size-parameter", "size"),
                environment.getProperty("spring.data.web.sort.sort-parameter", "sort"));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(RsqlFilterArgumentResolver.class)
    static class RsqlConfiguration {

        static {
            // The Specification is built from the filter parameter, it is not a request parameter itself.
            SpringDocUtils.getConfig().addRequestWrapperToIgnore(Specification.class);
        }

        @Bean
        @ConditionalOnMissingBean
        RsqlOperationCustomizer rsqlOperationCustomizer(RsqlFilterArgumentResolver resolver) {
            return new RsqlOperationCustomizer(resolver);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty(name = "api-core.security.enabled", matchIfMissing = true)
    static class SecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "apiCoreSecuritySchemeCustomizer")
        OpenApiCustomizer apiCoreSecuritySchemeCustomizer() {
            return openApi -> {
                if (openApi.getComponents() == null) {
                    openApi.setComponents(new Components());
                }
                openApi.getComponents().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
                openApi.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
            };
        }
    }
}
