package io.github.matheuslealpa.apicore.observability;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Registers opinionated defaults with the lowest precedence: any value in {@code application.yml},
 * environment variables or application arguments wins. Turn off with {@code api-core.defaults.enabled=false}.
 */
public class ApiCoreDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "apiCoreDefaults";

    static final Map<String, Object> DEFAULTS = Map.ofEntries(
            // Actuator
            Map.entry("management.endpoints.web.exposure.include", "health,info,metrics,prometheus"),
            Map.entry("management.endpoint.health.probes.enabled", "true"),
            Map.entry("management.endpoint.health.show-details", "when-authorized"),
            Map.entry("management.info.env.enabled", "true"),
            // Metrics: Prometheus via scrape; OTLP is used for traces only
            Map.entry("management.metrics.tags.application", "${spring.application.name:application}"),
            Map.entry("management.otlp.metrics.export.enabled", "false"),
            // Tracing / observations
            Map.entry("management.tracing.sampling.probability", "1.0"),
            Map.entry("management.observations.annotations.enabled", "true"),
            // Runtime
            Map.entry("spring.threads.virtual.enabled", "true"),
            Map.entry("spring.jpa.open-in-view", "false"),
            Map.entry("spring.mvc.problemdetails.enabled", "true"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("api-core.defaults.enabled", Boolean.class, true)) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }
}
