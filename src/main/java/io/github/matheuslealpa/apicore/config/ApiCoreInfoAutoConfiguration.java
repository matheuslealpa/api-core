package io.github.matheuslealpa.apicore.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * api-core startup log. To silence it: {@code logging.level.io.github.matheuslealpa.apicore.config=warn}.
 */
@AutoConfiguration
@EnableConfigurationProperties(ApiCoreProperties.class)
public class ApiCoreInfoAutoConfiguration {

    @Bean
    public ApiCoreStartupLogger apiCoreStartupLogger(ApiCoreProperties properties) {
        return new ApiCoreStartupLogger(properties);
    }
}
