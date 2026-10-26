package io.github.matheuslealpa.apicore.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Registers the {@link GlobalExceptionHandler}. An application-defined {@link ResponseEntityExceptionHandler} replaces it.
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ResponseEntityExceptionHandler.class, AccessDeniedException.class})
@ConditionalOnBooleanProperty(name = "api-core.web.enabled", matchIfMissing = true)
public class ApiCoreWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    public GlobalExceptionHandler apiCoreGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
