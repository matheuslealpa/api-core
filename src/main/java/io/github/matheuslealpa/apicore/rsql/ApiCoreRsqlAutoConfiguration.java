package io.github.matheuslealpa.apicore.rsql;

import java.util.List;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;
import io.github.perplexhub.rsql.RSQLJPASupport;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Resolves {@code Specification<T>} controller parameters from RSQL expressions; see {@link RsqlFilter}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RSQLJPASupport.class, WebMvcConfigurer.class})
@ConditionalOnBooleanProperty(name = "api-core.rsql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreRsqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RsqlFilterArgumentResolver rsqlFilterArgumentResolver(ApiCoreProperties properties, Environment environment,
            ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        String sortParam = environment.getProperty("spring.data.web.sort.sort-parameter", "sort");
        return new RsqlFilterArgumentResolver(properties.rsql().paramName(), sortParam,
                new JpaBasicAttributes(entityManagerFactory));
    }

    @Bean
    public WebMvcConfigurer apiCoreRsqlWebMvcConfigurer(RsqlFilterArgumentResolver resolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
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
