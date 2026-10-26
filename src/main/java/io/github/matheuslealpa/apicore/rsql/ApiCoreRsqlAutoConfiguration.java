package io.github.matheuslealpa.apicore.rsql;

import java.util.List;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;
import io.github.perplexhub.rsql.RSQLJPASupport;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita {@link RsqlFilter} nos controllers.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RSQLJPASupport.class, WebMvcConfigurer.class})
@ConditionalOnBooleanProperty(name = "api-core.rsql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreRsqlAutoConfiguration {

    @Bean
    public WebMvcConfigurer apiCoreRsqlWebMvcConfigurer(ApiCoreProperties properties) {
        RsqlFilterArgumentResolver resolver = new RsqlFilterArgumentResolver(properties.rsql().paramName());
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(resolver);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public RsqlExceptionHandler rsqlExceptionHandler() {
        return new RsqlExceptionHandler();
    }
}
