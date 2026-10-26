package io.github.matheuslealpa.apicore.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ApiCoreStartupLoggerTests {

    @Test
    void logaFeaturesLigadas(CapturedOutput output) {
        new ApiCoreStartupLogger(bind(Map.of())).logStartup();

        assertThat(output).contains("api-core dev started:",
                "\tCache: on (default spec: maximumSize=1000,expireAfterWrite=10m, caches: [])",
                "\tSecurity: jwt (roles claim: roles",
                "\tRSQL filters: on (param: filter)");
    }

    @Test
    void logaFeaturesDesligadas(CapturedOutput output) {
        new ApiCoreStartupLogger(bind(Map.of("api-core.security.enabled", "false", "api-core.cache.enabled", "false")))
                .logStartup();

        assertThat(output).contains("\tCache: off", "\tSecurity: open (no authentication)");
    }

    private static ApiCoreProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("api-core", ApiCoreProperties.class);
    }
}
