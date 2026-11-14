package io.github.matheuslealpa.apicore.web;

import io.github.matheuslealpa.apicore.config.ApiCoreProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Returns the trace id in the {@code api-core.web.trace-id-header} response header (default {@code X-Trace-Id}).
 * Set the property to an empty value to turn it off.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnExpression("'${api-core.web.trace-id-header:X-Trace-Id}' != ''")
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreTraceIdAutoConfiguration {

    /**
     * Right after Spring's {@code ServerHttpObservationFilter} ({@code HIGHEST_PRECEDENCE + 1}) and before Spring
     * Security, so 401/403 responses carry the header too.
     */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

    @Bean
    @ConditionalOnMissingBean(name = "apiCoreTraceIdResponseFilter")
    public FilterRegistrationBean<TraceIdResponseFilter> apiCoreTraceIdResponseFilter(ApiCoreProperties properties) {
        FilterRegistrationBean<TraceIdResponseFilter> registration =
                new FilterRegistrationBean<>(new TraceIdResponseFilter(properties.web().traceIdHeader()));
        registration.setOrder(ORDER);
        return registration;
    }
}
