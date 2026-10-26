package io.github.matheuslealpa.apicore.config;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Prints the api-core banner right below the Spring Boot banner. Honors {@code spring.main.banner-mode}
 * and can be turned off with {@code api-core.banner.enabled=false}.
 */
public class ApiCoreBannerPrinter implements ApplicationListener<ApplicationContextInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApiCoreBannerPrinter.class);

    private static final String[] BANNER = {
            "               _",
            "   __ _ _ __  (_)       ___ ___  _ __ ___",
            "  / _` | '_ \\ | |_____ / __/ _ \\| '__/ _ \\",
            " | (_| | |_) || |_____| (_| (_) | | |  __/",
            "  \\__,_| .__/ |_|      \\___\\___/|_|  \\___|",
            "       |_|"
    };

    private static final String NAME = " :: api-core :: ";

    private static final int STRAP_LINE_SIZE = 42;

    private final PrintStream out;

    public ApiCoreBannerPrinter() {
        this(System.out);
    }

    ApiCoreBannerPrinter(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        String mode = environment.getProperty("spring.main.banner-mode", "console");
        if ("off".equalsIgnoreCase(mode)
                || !environment.getProperty("api-core.banner.enabled", Boolean.class, true)) {
            return;
        }
        String banner = banner();
        if ("log".equalsIgnoreCase(mode)) {
            log.info("\n{}", banner);
        }
        else {
            out.println(banner);
        }
    }

    static String banner() {
        String version = "(v" + ApiCoreVersion.get() + ")";
        String padding = " ".repeat(Math.max(1, STRAP_LINE_SIZE - NAME.length() - version.length()));
        return String.join(System.lineSeparator(), BANNER) + System.lineSeparator()
                + System.lineSeparator() + NAME + padding + version + System.lineSeparator();
    }
}
