package io.github.matheuslealpa.apicore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Logs the api-core version and enabled features once the application is ready.
 */
public class ApiCoreStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(ApiCoreStartupLogger.class);

    private final ApiCoreProperties properties;

    public ApiCoreStartupLogger(ApiCoreProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartup() {
        if (!log.isInfoEnabled()) {
            return;
        }
        ApiCoreProperties.Cache cache = properties.cache();
        ApiCoreProperties.Security security = properties.security();
        log.info("""
                api-core {} started:
                \tCache: {}
                \tAudit: {}
                \tSecurity: {}
                \tRSQL filters: {}
                \tError handler: {}
                \tDefaults: {}""",
                ApiCoreVersion.get(),
                cache.enabled() ? "on (default spec: " + cache.defaultSpec() + ", caches: " + cache.specs().keySet() + ")" : "off",
                properties.audit().enabled() ? "on (system auditor: " + properties.audit().systemAuditor() + ")" : "off",
                security.enabled() ? "jwt (roles claim: " + security.rolesClaim() + ", public paths: " + security.publicPaths() + ")"
                        : "open (no authentication)",
                properties.rsql().enabled() ? "on (param: " + properties.rsql().paramName() + ")" : "off",
                onOff(properties.web().enabled()),
                onOff(properties.defaults().enabled()));
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }
}
